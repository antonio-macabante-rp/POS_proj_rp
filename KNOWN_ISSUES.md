# Known Issues and Limitations

## Current Limitations

### 1. Single Transaction Only
- System only supports one active transaction at a time
- No transaction history or reporting
- Cannot review previous sales

### 2. No Payment Processing
- Subtotal display only
- No cash, card, or payment handling
- No change calculation
- No receipt printing

### 3. Limited Item Management
- Cannot void individual items
- Cannot modify quantities
- Cannot apply discounts
- No item lookup/search functionality

### 4. Scanner Input
- Only tested with keyboard simulation
- Physical scanner compatibility depends on HID keyboard mode
- No configuration for scanner-specific settings

### 5. Data Persistence
- Journal logs are session-only (not saved to file)
- No transaction history database
- Price book must be reloaded on each startup (though database persists)

### 6. Error Handling
- Limited validation on TSV file format
- No recovery from database connection loss
- No handling of duplicate UPC scans in same transaction

### 7. UI Limitations
- Fixed window size (not responsive)
- No accessibility features (screen reader support, etc.)
- Limited error feedback (dialog boxes only)

## Future Enhancements

See Phase 4-5 discussion for potential improvements:
- File-based journal logging
- Transaction history
- Item void/modify functions
- Payment processing
- Receipt generation
- Multi-transaction support