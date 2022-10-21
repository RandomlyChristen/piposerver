# MyPainting - REST API server
마이페인팅 - 사용자화 컬러링 도안 제작 서비스 API

requirements 
- CMake >= 3.10.2
- JDK >= 1.8
- OpenCV == 4.5.5 compatible

## Auth API
Google ID Token을 받아 API 호출에 사용되는 JWT 토큰을 반환.

### Request
```
GET /user/auth
X-Auth-Token: {X-Auth-Token}
Content-Type: application/json
```

### Header
|Name|Description|
|---|---|
|X-Auth-Token|Google OAuth2 사용자 Login ID 토큰|

### Example
```
curl -X GET {BASE_URL}/user/auth \
     -H 'X-Auth-Token: {X-Auth-Token}' \
     -H 'Content-Type: application/json'
```

### Response
|Name|Description|
|---|---|
|data.name|Google 프로필에 등록된 사용자 이름|
|data.jwt|사용자 인증을 위해 API에 사용되는 JWT 토큰|

### Example
```
{
    "error": null,
    "data": {
        "name": "이수균",
        "jwt": "eyJhbGciOiJIUzI..."
    }
}
```

<br>

## List API
사용자가 등록한 모든 도안 정보를 반환

### Request
```
GET /pipo/list
Authorization: Bearer {JWT}
Content-Type: application/json
```

### Header
|Name|Description|
|---|---|
|Authorization|Auth API를 사용해 발급받은 JWT 토큰|

### Example
```
curl -X GET {BASE_URL}/pipo/list \
     -H 'Authorization: Bearer {JWT}' \
     -H 'Content-Type: application/json'
```

### Response
|Name|Type|Description|
|---|---|---|
|data|Array of [Pipo](#Pipo)|사용자가 등록한 모든 도안 정보|

### Example
```
{
    "error": null,
    "data": [
        {
            "id": "ff80808183f0cfcd0183f0d2e2a90000",
            "userId": "104113541111119136566",
            "createdAt": "2022-10-20T00:18:15.970639",
            "temp": false,
            "processed": true,
            "type": "jpeg",
            "difficulty": 3
        },
        {
            "id": "ff80808183f0cfcd0183f0d5cdaf0001",
            "userId": "104113541111119136566",
            "createdAt": "2022-10-20T00:21:27.214671",
            "temp": false,
            "processed": false,
            "type": "jpeg",
            "difficulty": 0
        }
    ]
}
```

<br>

## New API
새로운 도안의 원본 이미지 업로드. 만약 인증정보를 생략하는 경우, 임시파일로 등록됨.

### Request
```
POST /pipo/new
Authorization: Bearer {JWT}  # optional
Content-Type: multipart/form-data
```

### Header
|Name|Description|
|---|---|
|Authorization (optional)|Auth API를 사용해 발급받은 JWT 토큰|

### Parameter
|Name|Description|
|---|---|
|file|MIME type이 image/png, image/gif, image/jpeg, image/bmp의 이미지 파일|

### Example
```
curl -X POST {BASE_URL}/pipo/new \
     -H 'Authorization: Bearer {JWT}' \
     -H 'Content-Type: multipart/form-data'
     -F 'file={FILE}'
```
![New image](img/origin.jpeg)

### Response
|Name|Type|Description|
|---|---|---|
|data|[Pipo](#Pipo)|업로드한 원본이미지의 새로운 도안 정보|

### Example
```
{
    "error": null,
    "data": {
        "id": "ff80808183f0cfcd0183f0d5cdaf0001",
        "userId": "104113541111119136566",
        "createdAt": "2022-10-20T00:21:27.214671",
        "temp": false,
        "processed": false,
        "type": "jpeg",
        "difficulty": 0
    }
}
```

<br>

## Process API
업로드 되어 있는 원본 이미지를 원하는 난이도(쉬움, 중간, 어려움)을 사용하여 도안을 제작하여 서버에 저장.

### Request
```
PUT /pipo/process
Authorization: Bearer {JWT}  # optional
Content-Type: application/json
```

### Header
|Name|Description|
|---|---|
|Authorization (optional)|Auth API를 사용해 발급받은 JWT 토큰|

### Parameter
|Name|Description|
|---|---|
|id|도안을 제작할 서버에 업로드 되어 있는 [Pipo](#Pipo)의 id|
|difficulty|도안을 제작할 새로운 difficulty(1, 2, 3 중의 하나의 값).|

### Example
```
curl -X PUT {BASE_URL}/pipo/process \
     -H 'Authorization: Bearer {JWT}' \
     -H 'Content-Type: application/json' \
     -d '{
        "id": "ff80808183f0cfcd0183f0d5cdaf0001",
        "difficulty": 3
     }'
```

### Response
|Name|Type|Description|
|---|---|---|
|data|[Pipo](#Pipo)|제작을 완료한 후의 도안 정보|

### Example
```
{
    "error": null,
    "data": {
        "id": "ff80808183f0cfcd0183f0d5cdaf0001",
        "userId": "104113541111119136566",
        "createdAt": "2022-10-20T00:21:27.214671",
        "temp": false,
        "processed": true,
        "type": "jpeg",
        "difficulty": 3
    }
}
```

<br>

## Image API
New API를 통해 업로드 되어 있는 원본 이미지나, Process API로 처리된 도안 또는 미리보기 이미지를 반환.

### Request
```
GET /pipo/{origin|processed|preview}/{id}
Authorization: Bearer {JWT}  # optional
Content-Type: application/json
```

### Header
|Name|Description|
|---|---|
|Authorization (optional)|Auth API를 사용해 발급받은 JWT 토큰|

### Parameter
|Name|Description|
|---|---|
|origin|서버에 저장된 원본 이미지를 가져오기 위한 URI Path 변수|
|processed|서버에 저장된 도안 이미지를 가져오기 위한 URI Path 변수|
|preview|서버에 저장된 미리보기 이미지를 가져오기 위한 URI Path 변수|
|id|원본, 도안 또는 미리보기 이미지를 가져올 [Pipo](#Pipo)의 id|

### Example
```
curl -X GET {BASE_URL}/pipo/preview/ff80808183f0cfcd0183f0d5cdaf0001 \
     -H 'Authorization: Bearer {JWT}' \
     -H 'Content-Type: application/json'
```

### Response

### Example
![Image response ex](img/response.jpeg)

<br>

## Pipo
서버에 업로드 된 도안의 정보를 나타내는 자료형.

### Attributes
|Name|Type|Description|
|---|---|---|
|id|String|New API를 통해 발급된 도안의 unique ID|
|userId|String (Optional)|도안에 사용자를 구분하는 unique ID. 게스트 유저가 생성하여 임시 파일인 경우에는 유효하지 않음.|
|createdAt|Datetime|New API를 등록된 날짜와 시간|
|temp|Bool|게스트 유저가 생성하여 임시 파일인 경우에 true 아니면 false|
|processed|Bool|Process API를 사용하여 도안을 제작한 경우 true 아니면 false|
|type|String|jpeg, png, bmp, gif 등 원본 파일의 형식|
|difficulty|Integer|초기값은 0. Process API를 사용하여 도안을 제작한 경우 제작시 사용된 난이도(1~3)|

### Example

```
{
    "id": "ff80808183f0cfcd0183f0d5cdaf0001",
    "userId": "104113541111119136566",
    "createdAt": "2022-10-20T00:21:27.214671",
    "temp": false,
    "processed": true,
    "type": "jpeg",
    "difficulty": 3
}
```