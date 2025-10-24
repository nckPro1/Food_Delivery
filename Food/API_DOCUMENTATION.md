# 📱 **API Documentation cho Mobile App**

## 🚀 **Location & Shipping APIs**

### **Base URL:** `http://localhost:8080/api/app`

---

## 1. **Tính Phí Ship** 
### `POST /api/app/calculate-shipping`

**Mô tả:** Tính phí ship dựa trên địa chỉ giao hàng và giá trị đơn hàng

**Request Body:**
```json
{
  "orderAmount": 150000,
  "deliveryLatitude": 10.7769,
  "deliveryLongitude": 106.7009
}
```

**Response:**
```json
{
  "success": true,
  "message": "Tính phí ship thành công",
  "data": {
    "shippingFee": 25000,
    "distanceKm": 5.2,
    "estimatedDurationMinutes": 15,
    "fromCache": false
  }
}
```

---

## 2. **Kiểm Tra Khả Năng Giao Hàng**
### `POST /api/app/check-delivery-availability`

**Mô tả:** Kiểm tra địa chỉ có thể giao hàng không (max 50km)

**Request Body:**
```json
{
  "latitude": 10.7769,
  "longitude": 106.7009
}
```

**Response:**
```json
{
  "success": true,
  "message": "Kiểm tra khả năng giao hàng thành công",
  "data": {
    "isDeliverable": true,
    "distanceKm": 5.2,
    "estimatedDurationMinutes": 15,
    "message": "Có thể giao hàng"
  }
}
```

---

## 3. **Lấy Thông Tin Cửa Hàng**
### `GET /api/app/store-info`

**Mô tả:** Lấy thông tin cửa hàng bao gồm tọa độ và đơn hàng tối thiểu

**Response:**
```json
{
  "success": true,
  "message": "Lấy thông tin cửa hàng thành công",
  "data": {
    "storeName": "Nhà hàng ABC",
    "storeAddress": "123 Nguyễn Huệ, Quận 1, TP.HCM",
    "storePhone": "0123456789",
    "storeLatitude": 10.7769,
    "storeLongitude": 106.7009,
    "minOrderAmount": 50000
  }
}
```

---

## 4. **Tìm Kiếm Địa Chỉ Gần**
### `POST /api/app/search-nearby-addresses`

**Mô tả:** Tìm kiếm địa chỉ gần cửa hàng với thông tin khoảng cách

**Request Body:**
```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "radiusKm": 10
}
```

**Response:**
```json
{
  "success": true,
  "message": "Tìm kiếm địa chỉ thành công",
  "data": {
    "addresses": [
      {
        "address": "Địa chỉ hiện tại",
        "latitude": 10.7769,
        "longitude": 106.7009,
        "distanceKm": 0.0,
        "estimatedDurationMinutes": 0
      }
    ],
    "totalCount": 1
  }
}
```

---

## 🔧 **Android Integration Example**

### **1. Dependencies (build.gradle):**
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.google.android.gms:play-services-location:21.0.1'
implementation 'com.google.android.gms:play-services-maps:18.1.0'
```

### **2. API Service Interface:**
```java
public interface LocationApiService {
    @POST("api/app/calculate-shipping")
    Call<ApiResponse<ShippingCalculationResponse>> calculateShipping(
        @Body ShippingCalculationRequest request
    );
    
    @POST("api/app/check-delivery-availability")
    Call<ApiResponse<DeliveryAvailabilityResponse>> checkDeliveryAvailability(
        @Body DeliveryLocationRequest request
    );
    
    @GET("api/app/store-info")
    Call<ApiResponse<StoreInfoResponse>> getStoreInfo();
}
```

### **3. Location Permission (AndroidManifest.xml):**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

### **4. Usage Example:**
```java
// Get current location
LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

// Calculate shipping fee
ShippingCalculationRequest request = new ShippingCalculationRequest();
request.setOrderAmount(new BigDecimal("150000"));
request.setDeliveryLatitude(new BigDecimal(location.getLatitude()));
request.setDeliveryLongitude(new BigDecimal(location.getLongitude()));

Call<ApiResponse<ShippingCalculationResponse>> call = apiService.calculateShipping(request);
call.enqueue(new Callback<ApiResponse<ShippingCalculationResponse>>() {
    @Override
    public void onResponse(Call<ApiResponse<ShippingCalculationResponse>> call, 
                          Response<ApiResponse<ShippingCalculationResponse>> response) {
        if (response.isSuccessful() && response.body().isSuccess()) {
            ShippingCalculationResponse data = response.body().getData();
            // Update UI with shipping fee
            updateShippingFee(data.getShippingFee(), data.getDistanceKm());
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<ShippingCalculationResponse>> call, Throwable t) {
        // Handle error
    }
});
```

---

## 📋 **Data Models**

### **ShippingCalculationRequest:**
```java
public class ShippingCalculationRequest {
    private BigDecimal orderAmount;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
}
```

### **ShippingCalculationResponse:**
```java
public class ShippingCalculationResponse {
    private BigDecimal shippingFee;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private boolean fromCache;
}
```

### **DeliveryAvailabilityResponse:**
```java
public class DeliveryAvailabilityResponse {
    private boolean isDeliverable;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private String message;
}
```

### **StoreInfoResponse:**
```java
public class StoreInfoResponse {
    private String storeName;
    private String storeAddress;
    private String storePhone;
    private BigDecimal storeLatitude;
    private BigDecimal storeLongitude;
    private BigDecimal minOrderAmount;
}
```

---

## 🎯 **App Workflow**

### **1. Location Selection Flow:**
```
1. User opens location picker
2. App gets current location (GPS)
3. Call /api/app/check-delivery-availability
4. If deliverable → Show address
5. If not deliverable → Show error message
```

### **2. Shipping Calculation Flow:**
```
1. User adds items to cart
2. User selects delivery address
3. Call /api/app/calculate-shipping
4. Display shipping fee and estimated time
5. User confirms order
```

### **3. Store Information Flow:**
```
1. App starts
2. Call /api/app/store-info
3. Display store info and min order amount
4. Show store location on map
```

---

## 🚨 **Error Handling**

### **Common Error Responses:**
```json
{
  "success": false,
  "message": "Lỗi tính phí ship: Nhà hàng chưa được cấu hình tọa độ"
}
```

### **Error Codes:**
- **400**: Bad Request (invalid coordinates)
- **500**: Internal Server Error (API issues)
- **404**: Store settings not found

---

## 🧪 **Test URLs**

```
Test API Key: http://localhost:8080/api/test/google-api-key
Test COD Flow: http://localhost:8080/api/test/order-cod-flow
Store Info: http://localhost:8080/api/app/store-info
```

**Tất cả APIs đã sẵn sàng cho Mobile App integration!** 🚀

