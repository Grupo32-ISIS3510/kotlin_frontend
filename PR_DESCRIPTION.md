## Summary

- Extract duplicated OCR processing logic into shared `processScanResult()` in AddItemScreen
- Add warning logging for invalid expiry dates in ExpirationNotifier
- Add warning logging for partial bulk create failures in InventoryViewModel
- Return proper `Result.Error` for unimplemented delete/update endpoints in InventoryServiceAdapter
- Simplify redundant TOTAL pattern matching in ReceiptScanner
- Guard FCM token registration with null session token check in SecondServingApp

## Changes

| File | Change |
|------|--------|
| `SecondServingApp.kt` | Guard FCM registration with session token check |
| `ExpirationNotifier.kt` | Replace empty catch with proper warning log |
| `ReceiptScanner.kt` | Simplify redundant TOTAL pattern matching |
| `InventoryServiceAdapter.kt` | Return `Result.Error` for unimplemented endpoints |
| `AddItemScreen.kt` | Extract shared `processScanResult()` (~40 lines deduplicated) |
| `InventoryViewModel.kt` | Add logging for partial bulk create failures |

## Testing

- Built and installed on physical device (2312FPCA6G - Android 16)
- Verified app launches correctly with `adb reverse` for backend connectivity
