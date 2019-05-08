# Geogig.RepositoryStoresApi

All URIs are relative to *http://virtserver.swaggerhub.com/groldan/geogig-web/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**countStoreRepos**](RepositoryStoresApi.md#countStoreRepos) | **GET** /stores/{store}/repos/count | Return the number of repositories held on this store
[**createStore**](RepositoryStoresApi.md#createStore) | **POST** /stores | Creates a new repository store
[**getStore**](RepositoryStoresApi.md#getStore) | **GET** /stores/{store} | Get repository store info by name
[**getStores**](RepositoryStoresApi.md#getStores) | **GET** /stores | List summary information for available collections of repositories. Only stores visible to the current user are listed.
[**listStoreRepos**](RepositoryStoresApi.md#listStoreRepos) | **GET** /stores/{store}/repos | List summary information for all repositories on this repository store
[**modifyStore**](RepositoryStoresApi.md#modifyStore) | **PUT** /stores | Update repository info
[**removeStore**](RepositoryStoresApi.md#removeStore) | **DELETE** /stores/{store} | Remove the repository store configuration (disables all repositories on it)


<a name="countStoreRepos"></a>
# **countStoreRepos**
> &#39;Number&#39; countStoreRepos(store)

Return the number of repositories held on this store

### Example
```javascript
var Geogig = require('geogig');
var defaultClient = Geogig.ApiClient.instance;

// Configure API key authorization: ApiKeyAuth
var ApiKeyAuth = defaultClient.authentications['ApiKeyAuth'];
ApiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//ApiKeyAuth.apiKeyPrefix = 'Token';

// Configure HTTP basic authorization: BasicAuth
var BasicAuth = defaultClient.authentications['BasicAuth'];
BasicAuth.username = 'YOUR USERNAME';
BasicAuth.password = 'YOUR PASSWORD';

// Configure OAuth2 access token for authorization: OAuth2
var OAuth2 = defaultClient.authentications['OAuth2'];
OAuth2.accessToken = 'YOUR ACCESS TOKEN';

var apiInstance = new Geogig.RepositoryStoresApi();

var store = "store_example"; // String | Name of the repository store.

apiInstance.countStoreRepos(store).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **store** | **String**| Name of the repository store. | 

### Return type

**&#39;Number&#39;**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="createStore"></a>
# **createStore**
> StoreInfo createStore(storeInfo)

Creates a new repository store

### Example
```javascript
var Geogig = require('geogig');
var defaultClient = Geogig.ApiClient.instance;

// Configure API key authorization: ApiKeyAuth
var ApiKeyAuth = defaultClient.authentications['ApiKeyAuth'];
ApiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//ApiKeyAuth.apiKeyPrefix = 'Token';

// Configure HTTP basic authorization: BasicAuth
var BasicAuth = defaultClient.authentications['BasicAuth'];
BasicAuth.username = 'YOUR USERNAME';
BasicAuth.password = 'YOUR PASSWORD';

// Configure OAuth2 access token for authorization: OAuth2
var OAuth2 = defaultClient.authentications['OAuth2'];
OAuth2.accessToken = 'YOUR ACCESS TOKEN';

var apiInstance = new Geogig.RepositoryStoresApi();

var storeInfo = new Geogig.StoreInfo(); // StoreInfo | The repository to create

apiInstance.createStore(storeInfo).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **storeInfo** | [**StoreInfo**](StoreInfo.md)| The repository to create | 

### Return type

[**StoreInfo**](StoreInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/x-jackson-smile

<a name="getStore"></a>
# **getStore**
> StoreInfo getStore(store)

Get repository store info by name

### Example
```javascript
var Geogig = require('geogig');
var defaultClient = Geogig.ApiClient.instance;

// Configure API key authorization: ApiKeyAuth
var ApiKeyAuth = defaultClient.authentications['ApiKeyAuth'];
ApiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//ApiKeyAuth.apiKeyPrefix = 'Token';

// Configure HTTP basic authorization: BasicAuth
var BasicAuth = defaultClient.authentications['BasicAuth'];
BasicAuth.username = 'YOUR USERNAME';
BasicAuth.password = 'YOUR PASSWORD';

// Configure OAuth2 access token for authorization: OAuth2
var OAuth2 = defaultClient.authentications['OAuth2'];
OAuth2.accessToken = 'YOUR ACCESS TOKEN';

var apiInstance = new Geogig.RepositoryStoresApi();

var store = "store_example"; // String | Name of the repository store.

apiInstance.getStore(store).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **store** | **String**| Name of the repository store. | 

### Return type

[**StoreInfo**](StoreInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getStores"></a>
# **getStores**
> [StoreInfo] getStores()

List summary information for available collections of repositories. Only stores visible to the current user are listed.

### Example
```javascript
var Geogig = require('geogig');
var defaultClient = Geogig.ApiClient.instance;

// Configure API key authorization: ApiKeyAuth
var ApiKeyAuth = defaultClient.authentications['ApiKeyAuth'];
ApiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//ApiKeyAuth.apiKeyPrefix = 'Token';

// Configure HTTP basic authorization: BasicAuth
var BasicAuth = defaultClient.authentications['BasicAuth'];
BasicAuth.username = 'YOUR USERNAME';
BasicAuth.password = 'YOUR PASSWORD';

// Configure OAuth2 access token for authorization: OAuth2
var OAuth2 = defaultClient.authentications['OAuth2'];
OAuth2.accessToken = 'YOUR ACCESS TOKEN';

var apiInstance = new Geogig.RepositoryStoresApi();
apiInstance.getStores().then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters
This endpoint does not need any parameter.

### Return type

[**[StoreInfo]**](StoreInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listStoreRepos"></a>
# **listStoreRepos**
> [RepositoryInfo] listStoreRepos(store)

List summary information for all repositories on this repository store

### Example
```javascript
var Geogig = require('geogig');
var defaultClient = Geogig.ApiClient.instance;

// Configure API key authorization: ApiKeyAuth
var ApiKeyAuth = defaultClient.authentications['ApiKeyAuth'];
ApiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//ApiKeyAuth.apiKeyPrefix = 'Token';

// Configure HTTP basic authorization: BasicAuth
var BasicAuth = defaultClient.authentications['BasicAuth'];
BasicAuth.username = 'YOUR USERNAME';
BasicAuth.password = 'YOUR PASSWORD';

// Configure OAuth2 access token for authorization: OAuth2
var OAuth2 = defaultClient.authentications['OAuth2'];
OAuth2.accessToken = 'YOUR ACCESS TOKEN';

var apiInstance = new Geogig.RepositoryStoresApi();

var store = "store_example"; // String | Name of the repository store.

apiInstance.listStoreRepos(store).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **store** | **String**| Name of the repository store. | 

### Return type

[**[RepositoryInfo]**](RepositoryInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="modifyStore"></a>
# **modifyStore**
> StoreInfo modifyStore(storeInfo)

Update repository info

Updates a repository store

### Example
```javascript
var Geogig = require('geogig');
var defaultClient = Geogig.ApiClient.instance;

// Configure API key authorization: ApiKeyAuth
var ApiKeyAuth = defaultClient.authentications['ApiKeyAuth'];
ApiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//ApiKeyAuth.apiKeyPrefix = 'Token';

// Configure HTTP basic authorization: BasicAuth
var BasicAuth = defaultClient.authentications['BasicAuth'];
BasicAuth.username = 'YOUR USERNAME';
BasicAuth.password = 'YOUR PASSWORD';

// Configure OAuth2 access token for authorization: OAuth2
var OAuth2 = defaultClient.authentications['OAuth2'];
OAuth2.accessToken = 'YOUR ACCESS TOKEN';

var apiInstance = new Geogig.RepositoryStoresApi();

var storeInfo = new Geogig.StoreInfo(); // StoreInfo | The repository to update

apiInstance.modifyStore(storeInfo).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **storeInfo** | [**StoreInfo**](StoreInfo.md)| The repository to update | 

### Return type

[**StoreInfo**](StoreInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/x-jackson-smile

<a name="removeStore"></a>
# **removeStore**
> removeStore(store)

Remove the repository store configuration (disables all repositories on it)

### Example
```javascript
var Geogig = require('geogig');
var defaultClient = Geogig.ApiClient.instance;

// Configure API key authorization: ApiKeyAuth
var ApiKeyAuth = defaultClient.authentications['ApiKeyAuth'];
ApiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//ApiKeyAuth.apiKeyPrefix = 'Token';

// Configure HTTP basic authorization: BasicAuth
var BasicAuth = defaultClient.authentications['BasicAuth'];
BasicAuth.username = 'YOUR USERNAME';
BasicAuth.password = 'YOUR PASSWORD';

// Configure OAuth2 access token for authorization: OAuth2
var OAuth2 = defaultClient.authentications['OAuth2'];
OAuth2.accessToken = 'YOUR ACCESS TOKEN';

var apiInstance = new Geogig.RepositoryStoresApi();

var store = "store_example"; // String | Name of the repository store.

apiInstance.removeStore(store).then(function() {
  console.log('API called successfully.');
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **store** | **String**| Name of the repository store. | 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

