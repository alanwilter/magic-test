package fit.magic.cv.repcounter

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseRepCounterImplTest {

    private val exerciseRepCounter = ExerciseRepCounterImpl()

    @Test
    fun calculateAngleRightAngleReturns90Degrees() {
        val first = NormalizedLandmark.create(0f, 0f, 0f)
        val middle = NormalizedLandmark.create(0f, 1f, 0f)
        val last = NormalizedLandmark.create(1f, 1f, 0f)

        val angle = exerciseRepCounter.calculateAngle(first, middle, last)

        assertEquals(90f, angle, 0.1f)
    }

    @Test
    fun calculateAngleStraightLineReturns180Degrees() {
        val first = NormalizedLandmark.create(0f, 0f, 0f)
        val middle = NormalizedLandmark.create(1f, 0f, 0f)
        val last = NormalizedLandmark.create(2f, 0f, 0f)

        val angle = exerciseRepCounter.calculateAngle(first, middle, last)

        assertEquals(180f, angle, 0.1f)
    }

    @Test
    fun calculateAngleAcuteAngleReturns135Degrees() {
        val first = NormalizedLandmark.create(0f, 0f, 0f)
        val middle = NormalizedLandmark.create(1f, 1f, 0f)
        val last = NormalizedLandmark.create(2f, 1f, 0f)

        val angle = exerciseRepCounter.calculateAngle(first, middle, last)

        assertEquals(135f, angle, 0.1f)
    }

    @Test
    fun calculateAngleObtuseAngleReturns270Degrees() {
        val first = NormalizedLandmark.create(0f, 0f, 0f)
        val middle = NormalizedLandmark.create(1f, 1f, 0f)
        val last = NormalizedLandmark.create(0f, 2f, 0f)

        val angle = exerciseRepCounter.calculateAngle(first, middle, last)

        assertEquals(270f, angle, 0.1f)
    }

    @Test
    fun testIsLungePositionWithinTolerance() {
        val shoulderHipKneeAngle = 90f
        val hipKneeAnkleAngle = 90f
        assertTrue(
                "Expected to be lunge position when angles match the target.",
                exerciseRepCounter.isLungePosition(shoulderHipKneeAngle, hipKneeAnkleAngle)
        )
    }

    @Test
    fun testIsLungePositionOutsideTolerance() {
        val shoulderHipKneeAngle = 50f
        val hipKneeAnkleAngle = 130f
        assertFalse(
                "Expected not to be lunge position when angles are outside the tolerance.",
                exerciseRepCounter.isLungePosition(shoulderHipKneeAngle, hipKneeAnkleAngle)
        )
    }

    @Test
    fun testIsLungePositionOnTheEdgeOfTolerance() {
        val shoulderHipKneeAngle = 90f + ExerciseRepCounterImpl.ANGLE_TOLERANCE - 0.1f
        val hipKneeAnkleAngle = 90f + ExerciseRepCounterImpl.ANGLE_TOLERANCE - 0.1f
        assertTrue(
                "Expected to be lunge position when angles are on the edge of tolerance.",
                exerciseRepCounter.isLungePosition(shoulderHipKneeAngle, hipKneeAnkleAngle)
        )

        val shoulderHipKneeAngleOut = 90f + ExerciseRepCounterImpl.ANGLE_TOLERANCE + 0.1f
        val hipKneeAnkleAngleOut = 90f + ExerciseRepCounterImpl.ANGLE_TOLERANCE + 0.1f
        assertFalse(
                "Expected not to be lunge position when angles are just outside tolerance.",
                exerciseRepCounter.isLungePosition(shoulderHipKneeAngleOut, hipKneeAnkleAngleOut)
        )
    }

    @Test
    fun testIsLungePosition_exactTargetWithoutTolerance() {
        val shoulderHipKneeAngle = ExerciseRepCounterImpl.TARGET_ANGLE
        val hipKneeAnkleAngle = ExerciseRepCounterImpl.TARGET_ANGLE
        assertTrue(
                "Expected to be lunge position when angles are exactly the target.",
                exerciseRepCounter.isLungePosition(shoulderHipKneeAngle, hipKneeAnkleAngle)
        )
    }

    @Test
    fun testIsLungePositionOneAngleWithinTolerance() {
        val shoulderHipKneeAngle = 90f
        val hipKneeAnkleAngle = 90f + ExerciseRepCounterImpl.ANGLE_TOLERANCE - 0.1f
        assertTrue(
                "Expected to be lunge position when one angle is within tolerance.",
                exerciseRepCounter.isLungePosition(shoulderHipKneeAngle, hipKneeAnkleAngle)
        )

        val shoulderHipKneeAngleOut = 90f + ExerciseRepCounterImpl.ANGLE_TOLERANCE + 0.1f
        assertFalse(
                "Expected not to be lunge position when one angle is outside tolerance.",
                exerciseRepCounter.isLungePosition(shoulderHipKneeAngleOut, hipKneeAnkleAngle)
        )
    }

    @Test
    fun testCalculateProgressAtTargetAngle() {
        val angle = ExerciseRepCounterImpl.TARGET_ANGLE
        val expectedProgress = 1f
        assertEquals(
                "Progress should be max when angle is exactly the target.",
                expectedProgress,
                exerciseRepCounter.calculateProgress(angle),
                0.01f
        )
    }

    @Test
    fun testCalculateProgressWithinTolerance() {
        val angle = ExerciseRepCounterImpl.TARGET_ANGLE + ExerciseRepCounterImpl.ANGLE_TOLERANCE / 2
        val expectedProgress = 0.5f
        assertEquals(
                "Progress should be 0.5 when angle is halfway within tolerance.",
                expectedProgress,
                exerciseRepCounter.calculateProgress(angle),
                0.01f
        )
    }

    @Test
    fun testCalculateProgressOnEdgeOfTolerance() {
        val angle = ExerciseRepCounterImpl.TARGET_ANGLE + ExerciseRepCounterImpl.ANGLE_TOLERANCE
        val expectedProgress = 0f
        assertEquals(
                "Progress should be 0 on the edge of tolerance.",
                expectedProgress,
                exerciseRepCounter.calculateProgress(angle),
                0.01f
        )
    }

    @Test
    fun testCalculateProgressOutsideTolerance() {
        val angle =
                ExerciseRepCounterImpl.TARGET_ANGLE + ExerciseRepCounterImpl.ANGLE_TOLERANCE + 10f
        val expectedProgress = 0f
        assertEquals(
                "Progress should be 0 when angle is outside tolerance.",
                expectedProgress,
                exerciseRepCounter.calculateProgress(angle),
                0.01f
        )
    }

    @Test
    fun testCalculateProgressOppositeDirection() {
        val angle = ExerciseRepCounterImpl.TARGET_ANGLE - ExerciseRepCounterImpl.ANGLE_TOLERANCE / 2
        val expectedProgress = 0.5f
        assertEquals(
                "Progress should be 0.5 when angle is halfway within tolerance in the opposite direction.",
                expectedProgress,
                exerciseRepCounter.calculateProgress(angle),
                0.01f
        )
    }
}
