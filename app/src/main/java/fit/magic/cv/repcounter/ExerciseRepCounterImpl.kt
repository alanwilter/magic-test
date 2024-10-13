// Copyright (c) 2024 Magic Tech Ltd

package fit.magic.cv.repcounter

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import fit.magic.cv.PoseLandmarkerHelper
import kotlin.math.abs
import kotlin.math.atan2

class ExerciseRepCounterImpl : ExerciseRepCounter() {
    private var lastRightLegLunge = false
    private var lastLeftLegLunge = false
    private var currentProgress = 0f

    companion object {
        private const val TARGET_ANGLE = 90f
        private const val ANGLE_TOLERANCE = 21f // after looking the notebook
        private const val PROGRESS_SMOOTHING_FACTOR = 0.7f
        private const val MAX_PROGRESS = 1f
    }

    override fun setResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        val result = resultBundle.results.firstOrNull() ?: return
        val landmarks = result.landmarks().firstOrNull() ?: return

        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        val leftKnee = landmarks[25]
        val rightKnee = landmarks[26]
        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]

        val rightShoulderHipKneeAngle = calculateAngle(rightShoulder, rightHip, rightKnee)
        val rightHipKneeAnkleAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        val leftShoulderHipKneeAngle = calculateAngle(leftShoulder, leftHip, leftKnee)
        val leftHipKneeAnkleAngle = calculateAngle(leftHip, leftKnee, leftAnkle)

        val isRightLegLunge = isLungePosition(rightShoulderHipKneeAngle, rightHipKneeAnkleAngle)
        val isLeftLegLunge = isLungePosition(leftShoulderHipKneeAngle, leftHipKneeAnkleAngle)

        // Count repetitions
        if (isRightLegLunge && !lastRightLegLunge && !isLeftLegLunge) {
            incrementRepCount()
        } else if (isLeftLegLunge && !lastLeftLegLunge && !isRightLegLunge) {
            incrementRepCount()
        }

        lastRightLegLunge = isRightLegLunge
        lastLeftLegLunge = isLeftLegLunge

        // Update progress
        val rightProgress = calculateProgress(rightShoulderHipKneeAngle)
        val leftProgress = calculateProgress(leftShoulderHipKneeAngle)
        val newProgress = maxOf(rightProgress, leftProgress)

        // Smooth progress updates
        currentProgress =
                currentProgress * PROGRESS_SMOOTHING_FACTOR +
                        newProgress * (1 - PROGRESS_SMOOTHING_FACTOR)
        sendProgressUpdate(currentProgress)

        // Provide feedback
        provideFeedback(
                isRightLegLunge,
                isLeftLegLunge,
                rightShoulderHipKneeAngle,
                leftShoulderHipKneeAngle
        )
    }

    private fun calculateAngle(
            first: NormalizedLandmark,
            middle: NormalizedLandmark,
            last: NormalizedLandmark
    ): Float {
        val angle =
                Math.toDegrees(
                                (atan2(last.y() - middle.y(), last.x() - middle.x()) -
                                                atan2(
                                                        first.y() - middle.y(),
                                                        first.x() - middle.x()
                                                ))
                                        .toDouble()
                        )
                        .toFloat()
        return abs(if (angle < 0) angle + 360 else angle)
    }

    private fun isLungePosition(shoulderHipKneeAngle: Float, hipKneeAnkleAngle: Float): Boolean {
        return abs(shoulderHipKneeAngle - TARGET_ANGLE) < ANGLE_TOLERANCE &&
                abs(hipKneeAnkleAngle - TARGET_ANGLE) < ANGLE_TOLERANCE
    }

    private fun calculateProgress(angle: Float): Float {
        return maxOf(
                0f,
                minOf(
                        MAX_PROGRESS,
                        MAX_PROGRESS - abs(angle - TARGET_ANGLE) / ANGLE_TOLERANCE * MAX_PROGRESS
                )
        )
    }

    private fun provideFeedback(
            isRightLegLunge: Boolean,
            isLeftLegLunge: Boolean,
            rightAngle: Float,
            leftAngle: Float
    ) {
        when {
            isRightLegLunge && isLeftLegLunge -> sendFeedbackMessage("Keep one leg straight")
            isRightLegLunge -> sendFeedbackMessage("Good lunge position with right leg")
            isLeftLegLunge -> sendFeedbackMessage("Good lunge position with left leg")
            abs(rightAngle - TARGET_ANGLE) < abs(leftAngle - TARGET_ANGLE) ->
                    sendFeedbackMessage("Bend your right knee more")
            else -> sendFeedbackMessage("Bend your left knee more")
        }
    }
}
