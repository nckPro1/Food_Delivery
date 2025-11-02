# 💳 Payment System Documentation

## 🚀 Overview
Hệ thống thanh toán tích hợp VNPay Sandbox cho Food Delivery System.

## 🔧 Configuration

### VNPay Sandbox Settings
```properties
# Backend/src/main/resources/application.properties
vnpay.tmnCode=0HW0BKYF
vnpay.hashSecret=V5YO0RA4YJASU4GG4FYITSFCRFWY6G9T
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.returnUrl=http://localhost:8080/api/payment/vnpay/return
vnpay.cancelUrl=http://localhost:8080/api/payment/vnpay/cancel
```

## 📋 API Endpoints

### 1. Payment Methods
```http
GET /api/payment/methods
```
**Response:**
```json
{
  "success": true,
  "message": "Lấy danh sách phương thức thanh toán thành công",
  "data": [
    {
      "methodId": "VNPAY",
      "methodName": "VNPay",
      "description": "Thanh toán qua VNPay",
      "iconUrl": "/images/payment/vnpay.png",
      "isActive": true,
      "isOnline": true
    },
    {
      "methodId": "CASH",
      "methodName": "Tiền mặt",
      "description": "Thanh toán khi nhận hàng",
      "iconUrl": "/images/payment/cash.png",
      "isActive": true,
      "isOnline": false
    }
  ]
}
```

### 2. VNPay Banks
```http
GET /api/payment/vnpay/banks
```
**Response:**
```json
{
  "success": true,
  "message": "Lấy danh sách ngân hàng thành công",
  "data": [
    {
      "code": "NCB",
      "name": "Ngân hàng Quốc Dân (NCB)"
    },
    {
      "code": "SACOMBANK",
      "name": "Ngân hàng Sài Gòn Thương Tín (Sacombank)"
    }
  ]
}
```

### 3. Create Payment
```http
POST /api/payment/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "orderId": 1,
  "paymentMethod": "VNPAY",
  "amount": 150000,
  "description": "Thanh toan don hang ORD-123456",
  "returnUrl": "http://localhost:8080/api/payment/vnpay/return",
  "cancelUrl": "http://localhost:8080/api/payment/vnpay/cancel",
  "ipAddress": "127.0.0.1",
  "bankCode": "NCB"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Tạo thanh toán VNPay thành công",
  "data": {
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=15000000&vnp_Command=pay&...",
    "vnpTxnRef": "ORDER_1_1701427200000",
    "vnpOrderInfo": "Thanh toan don hang ORD-123456",
    "vnpAmount": "15000000",
    "vnpCreateDate": "20241201120000",
    "vnpExpireDate": "20241201121500",
    "vnpSecureHash": "abc123...",
    "vnpReturnUrl": "http://localhost:8080/api/payment/vnpay/return",
    "vnpCancelUrl": "http://localhost:8080/api/payment/vnpay/cancel"
  }
}
```

### 4. VNPay Callback
```http
GET /api/payment/vnpay/return?vnp_Amount=15000000&vnp_ResponseCode=00&...
```

### 5. Test Endpoints
```http
# Test tạo order và thanh toán
POST /api/test/payment/create-order-and-payment

# Test VNPay payment
POST /api/test/payment/vnpay/{orderId}?bankCode=NCB

# Test cash payment
POST /api/test/payment/cash/{orderId}

# Test VNPay callback
POST /api/test/payment/vnpay/callback
```

## 🔄 Payment Flow

### 1. VNPay Flow
```
1. User chọn VNPay → PaymentMethodActivity
2. Tạo payment request → PaymentController.createPayment()
3. VNPayService tạo payment URL
4. Mở WebView với VNPay URL → VNPayPaymentActivity
5. User thanh toán trên VNPay
6. VNPay callback → PaymentController.vnpayReturn()
7. Xử lý kết quả → PaymentSuccessActivity
```

### 2. Cash Flow
```
1. User chọn Cash → PaymentMethodActivity
2. Xác nhận thanh toán tiền mặt
3. Tạo payment với status COMPLETED
4. Cập nhật order status → CONFIRMED
5. Chuyển về màn hình chính
```

## 🧪 Testing

### Test VNPay Payment
```bash
# 1. Tạo order test
curl -X POST http://localhost:8080/api/test/payment/create-order-and-payment \
  -H "Content-Type: application/json" \
  -d '{}'

# 2. Test VNPay payment với order ID
curl -X POST http://localhost:8080/api/test/payment/vnpay/1?bankCode=NCB

# 3. Test callback simulation
curl -X POST http://localhost:8080/api/test/payment/vnpay/callback \
  -H "Content-Type: application/json" \
  -d '{
    "vnp_Amount": "15000000",
    "vnp_ResponseCode": "00",
    "vnp_TransactionStatus": "00",
    "vnp_TxnRef": "ORDER_1_1701427200000",
    "vnp_OrderInfo": "Test Payment",
    "vnp_BankCode": "NCB",
    "vnp_BankTranNo": "1234567890",
    "vnp_CardType": "ATM",
    "vnp_PayDate": "20241201120000",
    "vnp_TmnCode": "0HW0BKYF",
    "vnp_TransactionNo": "1234567890",
    "vnp_SecureHash": "test_hash",
    "vnp_SecureHashType": "SHA256"
  }'
```

## 📱 Android Integration

### 1. Payment Method Selection
```java
// PaymentMethodActivity.java
Intent intent = new Intent(this, PaymentMethodActivity.class);
intent.putExtra("orderNumber", orderNumber);
intent.putExtra("totalAmount", totalAmount.toString());
startActivity(intent);
```

### 2. VNPay Payment
```java
// VNPayPaymentActivity.java
WebView webView = findViewById(R.id.webViewPayment);
webView.loadUrl(paymentUrl);
```

### 3. Payment Success
```java
// PaymentSuccessActivity.java
Intent intent = new Intent(this, PaymentSuccessActivity.class);
intent.putExtra("orderNumber", orderNumber);
intent.putExtra("totalAmount", totalAmount.toString());
startActivity(intent);
```

## 🔒 Security Features

1. **HMAC SHA512 Hash Verification**
2. **IP Address Validation**
3. **Transaction ID Uniqueness**
4. **Callback Verification**
5. **Secure Token Generation**

## 📊 Database Schema

### Payment Table
```sql
CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'BANK_TRANSFER', 'E_WALLET') NOT NULL,
    payment_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    payment_amount DECIMAL(10, 2) NOT NULL,
    transaction_id VARCHAR(100),
    payment_notes TEXT,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
```

## 🚨 Error Handling

### Common Error Responses
```json
{
  "success": false,
  "message": "Lỗi tạo thanh toán: Order not found",
  "data": null
}
```

### Error Codes
- **400**: Bad Request (invalid parameters)
- **404**: Order not found
- **500**: Internal Server Error (VNPay integration issues)

## 🔧 Troubleshooting

### 1. VNPay URL Generation Failed
- Check VNPay configuration in application.properties
- Verify HMAC SHA512 hash generation
- Check parameter encoding

### 2. Callback Verification Failed
- Verify secure hash calculation
- Check parameter order and encoding
- Ensure VNPay secret key is correct

### 3. Payment Status Not Updated
- Check database transaction
- Verify order status update logic
- Check payment service logs

## 📈 Monitoring

### Logs to Monitor
```
- VNPay payment URL creation
- Payment callback processing
- Order status updates
- Payment completion
```

### Key Metrics
- Payment success rate
- VNPay callback response time
- Payment method distribution
- Failed payment reasons

## 🔄 Future Enhancements

1. **Multiple Payment Gateways** (MoMo, ZaloPay)
2. **Payment Refunds**
3. **Payment Analytics**
4. **Recurring Payments**
5. **Payment Notifications**
6. **Fraud Detection**

---

**Tác giả**: AI Assistant  
**Ngày tạo**: 2024-12-01  
**Phiên bản**: 1.0.0









































