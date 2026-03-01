# Krishva's Kitchen Server

## Purpose
- Powers menu, order, delivery, and notification flows for the Krishva's Kitchen app.

## User Flow Support

### 1. Menu Flow
- Shows today's menu to users.
- If today's menu is missing, shows the latest scheduled menu.
- Supports menu scheduling for upcoming days.

### 2. Order Flow
- Accepts cart checkout requests.
- Creates unique order IDs.
- Stores payment choice (COD/UPI).
- Shares order status updates for tracking.

### 3. Delivery Partner Flow
- Accepts delivery partner applications from users.
- Sends request to admin for approval.
- Updates partner status after admin decision.
- Stores live location updates for active orders.

### 4. Admin Flow
- Lets admin manage menus and scheduled menus.
- Lets admin manage orders and delivery assignments.
- Lets admin approve/reject delivery partner requests.
- Provides summary view for users, orders, and partner requests.

### 5. Notification Flow
- Sends real-time notifications for:
- new orders
- status updates
- delivery assignments
- delivery partner registrations
