# App API Documentation - Address & Shipping System

## Overview
Hệ thống địa chỉ và phí ship mới sử dụng thành phố/quận/phường thay vì GPS coordinates.

## API Endpoints

### 1. Lấy danh sách thành phố
```
GET /api/app/cities
```
**Response:**
```json
{
  "success": true,
  "message": "Lấy danh sách thành phố thành công",
  "data": [
    "ho-chi-minh",
    "ha-noi",
    "da-nang",
    "hai-phong",
    "can-tho",
    ...
  ]
}
```

### 2. Lấy danh sách quận/huyện theo thành phố
```
GET /api/app/districts?city=ho-chi-minh
```
**Response:**
```json
{
  "success": true,
  "message": "Lấy danh sách quận/huyện thành công",
  "data": [
    "quan-1",
    "quan-2",
    "quan-3",
    "quan-thu-duc",
    "quan-go-vap",
    ...
  ]
}
```

### 3. Lấy danh sách phường/xã theo quận/huyện
```
GET /api/app/wards?city=ho-chi-minh&district=quan-1
```
**Response:**
```json
{
  "success": true,
  "message": "Lấy danh sách phường/xã thành công",
  "data": [
    "phuong-1",
    "phuong-2",
    "phuong-3",
    "phuong-trung-tam",
    ...
  ]
}
```

### 4. Tính phí ship
```
POST /api/app/calculate-shipping
```
**Request:**
```json
{
  "orderAmount": 150000,
  "deliveryCity": "ho-chi-minh",
  "deliveryDistrict": "quan-1",
  "deliveryWard": "phuong-1",
  "deliveryStreet": "123 Nguyễn Huệ"
}
```
**Response:**
```json
{
  "success": true,
  "message": "Tính phí ship thành công",
  "data": {
    "shippingFee": 10000,
    "distanceKm": 0,
    "estimatedDurationMinutes": 30,
    "fromCache": false,
    "description": "Cùng quận"
  }
}
```

### 5. Kiểm tra khả năng giao hàng
```
POST /api/app/check-delivery-availability
```
**Request:**
```json
{
  "city": "ho-chi-minh",
  "district": "quan-1",
  "ward": "phuong-1",
  "street": "123 Nguyễn Huệ"
}
```
**Response:**
```json
{
  "success": true,
  "message": "Kiểm tra khả năng giao hàng thành công",
  "data": {
    "isDeliverable": true,
    "shippingFee": 10000,
    "estimatedDurationMinutes": 30,
    "message": "Có thể giao hàng"
  }
}
```

### 6. Lấy thông tin cửa hàng
```
GET /api/app/store-info
```
**Response:**
```json
{
  "success": true,
  "message": "Lấy thông tin cửa hàng thành công",
  "data": {
    "storeName": "Nhà hàng của tôi",
    "storePhone": "0123456789",
    "storeEmail": "info@myrestaurant.com",
    "storeCity": "ho-chi-minh",
    "storeDistrict": "quan-1",
    "storeWard": "Phường Bến Nghé",
    "storeStreet": "123 Nguyễn Huệ",
    "storeDescription": "Nhà hàng phục vụ các món ăn ngon và chất lượng"
  }
}
```

## User Profile API Updates

### Cập nhật thông tin user
```
PUT /api/user/profile
```
**Request:**
```json
{
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0123456789",
  "address": "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM",
  "userCity": "ho-chi-minh",
  "userDistrict": "quan-1",
  "userWard": "phuong-ben-nghe",
  "userStreet": "123 Nguyễn Huệ"
}
```

### Lấy thông tin user
```
GET /api/user/profile
```
**Response:**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0123456789",
  "address": "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM",
  "avatarUrl": "/uploads/avatars/avatar.jpg",
  "roleId": 1,
  "authProvider": "EMAIL",
  "userCity": "ho-chi-minh",
  "userDistrict": "quan-1",
  "userWard": "phuong-ben-nghe",
  "userStreet": "123 Nguyễn Huệ"
}
```

## Order API Updates

### Tạo đơn hàng
```
POST /api/orders
```
**Request:**
```json
{
  "userId": 1,
  "deliveryAddress": "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM",
  "deliveryNotes": "Giao hàng vào buổi tối",
  "deliveryCity": "ho-chi-minh",
  "deliveryDistrict": "quan-1",
  "deliveryWard": "phuong-ben-nghe",
  "deliveryStreet": "123 Nguyễn Huệ",
  "paymentMethod": "CASH",
  "orderItems": [
    {
      "productId": 1,
      "quantity": 2,
      "selectedOptions": [
        {
          "optionId": 1,
          "value": "Lớn"
        }
      ]
    }
  ]
}
```

## Shipping Fee Logic

### Cùng quận/huyện
- Phí ship: 10,000 VND (có thể điều chỉnh)
- Mô tả: "Cùng quận"

### Khác quận/huyện, cùng thành phố
- Phí ship: 30,000 VND (có thể điều chỉnh)
- Mô tả: "Khác quận"

### Ngoài thành phố
- Phí ship: 0 VND (không nhận đơn)
- Mô tả: "Không nhận đơn"

### Miễn phí ship
- Khi tổng giá trị đơn hàng >= ngưỡng miễn phí (mặc định 200,000 VND)
- Mô tả: "Miễn phí ship"

## Implementation Notes

1. **Address Components**: Sử dụng `city`, `district`, `ward`, `street` thay vì GPS coordinates
2. **Shipping Calculation**: Dựa trên so sánh địa chỉ cửa hàng và địa chỉ giao hàng
3. **Data Source**: Danh sách thành phố/quận/phường được hardcode trong API
4. **Fallback**: Nếu không tìm thấy quận/phường cụ thể, sử dụng danh sách mặc định
5. **Caching**: Có thể implement caching cho shipping calculation trong tương lai

## Error Handling

Tất cả API đều trả về format chuẩn:
```json
{
  "success": false,
  "message": "Mô tả lỗi chi tiết",
  "data": null
}
```

## Security

- Tất cả API `/api/app/**` đều public (không cần authentication)
- API `/api/user/**` cần authentication
- API `/api/orders` cần authentication cho tạo đơn hàng




