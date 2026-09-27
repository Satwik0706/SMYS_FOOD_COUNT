# Fix Breakfast Increment Logic for Lunch Box

The current implementation fails to correctly account for the extra breakfast portion required when a student selects the "Lunch Box" option. While the system correctly locks the lunch option, it only increments the breakfast count by 1 instead of the required 2 (one for normal breakfast and one for the lunch box).

## Proposed Changes

### Admin Component

#### [MODIFY] [AdminFoodCountViewModel.kt](file:///D:/oodapplication/app/src/main/java/com/satwik/oodapplication/presentation/admin/AdminFoodCountViewModel.kt)
- Update the aggregation logic in the `report` StateFlow to check for the `lunchBox` status.
- If a student has selected the `lunchBox` option, increment the breakfast count an additional time.
- This ensures that the total breakfast count correctly reflects the number of portions/packets the kitchen needs to prepare.

## Verification Plan

### Manual Verification
1.  **Student Side**:
    - Log in as a student.
    - Toggle "Breakfast" to ON.
    - Toggle "Lunch Box" to ON.
    - Verify that "Lunch" is automatically locked and set to OFF.
2.  **Admin Side**:
    - Log in as an admin.
    - Open the "Food Count Report".
    - Verify that for the student who selected both Breakfast and Lunch Box, the "Morning (B)" total count reflects an increment of 2.
    - Expand the batch breakdown and verify that the breakfast count for that specific batch also reflects the double increment.
    - Verify that for a student who only selected "Breakfast" (without Lunch Box), the increment is still 1.
