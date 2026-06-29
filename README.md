# 🏡 HOMEVERSE - SMART REAL ESTATE PLATFORM (BACKEND)

## 📖 Giới thiệu

**HOMEVERSE** là nền tảng bất động sản thông minh được xây dựng theo kiến trúc **Microservices**, hỗ trợ tìm kiếm, mua bán và cho thuê bất động sản trên nền tảng Web và Mobile.

Hệ thống tích hợp **AI Recommendation**, **Realtime Chat**, **Elasticsearch**, **PostGIS**, **VNPay**, **AI Chatbot** cùng nhiều công nghệ hiện đại nhằm mang lại trải nghiệm tìm kiếm và giao dịch bất động sản hiệu quả.

Repository này chứa toàn bộ **Backend API** được phát triển bằng **Spring Boot Microservices**, cung cấp dịch vụ cho Website, Mobile App và AI Services.

---

# 🎯 Mục tiêu

* Xây dựng hệ thống Microservices dễ mở rộng
* Tìm kiếm bất động sản theo vị trí địa lý
* Đề xuất bất động sản bằng AI Recommendation
* Chat Realtime giữa người mua và người bán
* Thanh toán trực tuyến qua VNPay
* Quản lý ví điện tử
* Hỗ trợ AI Chatbot
* Hỗ trợ Web và Mobile App

---

# 🏗️ Kiến trúc hệ thống

```
Website (ReactJS)        Mobile (React Native)
            │
            ▼
        Nginx Reverse Proxy
            │
            ▼
      Spring Boot Microservices
            │
 ┌───────────────────────────────────┐
 │ Identity Service                  │
 │ Customer Service                  │
 │ Property Service                  │
 │ Media Service                     │
 │ Search Service                    │
 │ Wallet Service                    │
 │ Payment Service                   │
 │ Chat Service                      │
 │ Notification Service              │
 │ Recommendation Service            │
 │ AI Worker Service                 │
 └───────────────────────────────────┘
            │
            ▼
 PostgreSQL • MongoDB • Redis
 Kafka • Elasticsearch • PostGIS
            │
            ▼
 Recommendation ML Service
     (FastAPI + LightGBM)
```

---

# ✨ Chức năng chính

## 🔐 Xác thực

* Đăng ký
* Đăng nhập
* OAuth2 Google
* JWT Authentication
* Phân quyền người dùng

---

## 🏠 Quản lý bất động sản

* Đăng tin bất động sản
* Chỉnh sửa bài đăng
* Xóa và khôi phục bài đăng
* Duyệt bài đăng
* Tin VIP
* Tin nổi bật
* Tin xu hướng
* Reels bất động sản

---

## 🔍 Tìm kiếm

* Tìm kiếm theo từ khóa
* Tìm kiếm theo khu vực
* Tìm kiếm theo PostGIS
* Gợi ý từ khóa
* Lưu lịch sử tìm kiếm
* Elasticsearch Full-text Search

---

## 🤖 AI Recommendation

* Hybrid Recommendation
* AI Recommendation bằng LightGBM
* Theo dõi hành vi người dùng
* User Interest Profile
* Realtime Recommendation
* Redis Cache
* Fraud Detection
* Multi-Armed Bandit

---

## 💬 Chat Realtime

* Chat 1-1
* WebSocket
* Typing Indicator
* Online/Offline
* Đã xem tin nhắn
* Thu hồi tin nhắn
* Trả lời tin nhắn
* Reaction Emoji
* Gửi hình ảnh
* AI Chat Assistant

---

## 💳 Ví điện tử

* Quản lý ví
* Nạp tiền
* Lịch sử giao dịch
* Thanh toán VNPay

---

## 🔔 Thông báo

* Notification Realtime
* Kafka Event
* Thông báo hệ thống

---

## 📊 Dashboard

* Thống kê hành vi người dùng
* Thống kê Recommendation
* Dashboard Admin
* Analytics

---

# 🚀 Công nghệ sử dụng

## Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* Spring Cloud OpenFeign
* REST API
* WebSocket (STOMP)

### Database

* PostgreSQL
* MongoDB
* Redis

### AI

* FastAPI
* Python
* LightGBM

### Search

* Elasticsearch
* PostGIS

### Message Broker

* Apache Kafka

### DevOps

* Docker
* Docker Compose
* Nginx
* GitHub Actions

### Monitoring

* Prometheus
* Grafana

---

# 📂 Cấu trúc dự án

```
Property_System/

├── identity-service
├── customer-service
├── property-service
├── media-service
├── search-service
├── wallet-service
├── payment-service
├── chat-service
├── notification-service
├── recommend-service
├── recommendation-ml-service
├── ai-worker-service
├── nginx
├── monitoring
├── docker-compose.yml
├── docker-compose.prod.yml
└── pom.xml
```

---

# 🔐 Phân quyền hệ thống

| Vai trò | Quyền                         |
| ------- | ----------------------------- |
| ADMIN   | Quản lý toàn bộ hệ thống      |
| USER    | Mua, bán, thuê bất động sản   |
| OWNER   | Đăng và quản lý bài đăng      |
| AGENT   | Quản lý bất động sản môi giới |

---

# ⚙️ Cài đặt

## Clone source

```bash
git clone https://github.com/Khannguyen-ui/Property_System.git
```

## Cấu hình

Cập nhật file:

```
application.yml
```

Cấu hình:

* PostgreSQL
* MongoDB
* Redis
* Kafka
* Elasticsearch
* Google OAuth
* Cloudinary
* Gemini API

---

## Build Project

```bash
mvn clean install
```

---

## Chạy bằng Docker

```bash
docker compose up -d --build
```

---

## Chạy từng Service

```bash
mvn spring-boot:run
```

---

# 📡 API

## Authentication

```
POST /auth/login
POST /auth/register
```

## Property

```
GET /public/properties
POST /api/properties
PUT /api/properties/{id}
DELETE /api/properties/{id}
```

## Recommendation

```
POST /recommend/track
GET /recommend/users/{id}/properties/final
GET /recommend/users/{id}/reels/final
```

## Chat

```
GET /api/chat/conversations
POST /api/chat/send
PUT /api/chat/read/{id}
```

## Wallet

```
GET /api/wallet
POST /api/wallet/deposit
```

---

# 🔮 Hướng phát triển

* Video Call
* AI Image Search
* AI Property Price Prediction
* AI Voice Assistant
* Recommendation Explainability
* Kubernetes Deployment
* Distributed Tracing
* Multi-language Support
* Mobile Push Notification

---

# 👨‍💻 Nhóm phát triển

**HOMEVERSE Development Team**

**Backend Repository**

https://github.com/Khannguyen-ui/Property_System

**Frontend Repository**

https://github.com/Khannguyen-ui/BDSforntend
