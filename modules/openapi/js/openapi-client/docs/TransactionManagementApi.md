# Geogig.TransactionManagementApi

All URIs are relative to *http://virtserver.swaggerhub.com/groldan/geogig-web/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**abortTransaction**](TransactionManagementApi.md#abortTransaction) | **POST** /transactions/{user}/{repo}/{transactionId}/abort | Abort transaction. Returns immediately.
[**commitTransaction**](TransactionManagementApi.md#commitTransaction) | **POST** /transactions/{user}/{repo}/{transactionId}/commit | Commit transaction. Async operation.
[**deleteTransactionInfo**](TransactionManagementApi.md#deleteTransactionInfo) | **DELETE** /transactions/{user}/{repo}/{transactionId} | Delete a transaction information that&#39;s finished but not yet expired.
[**getTransactionInfo**](TransactionManagementApi.md#getTransactionInfo) | **GET** /transactions/{user}/{repo}/{transactionId} | Obtain the current status of the given transaction
[**listAllTransactions**](TransactionManagementApi.md#listAllTransactions) | **GET** /transactions | List all non expired transactions open an all the repositories visible to the authenticated user
[**listRepositoryTransactions**](TransactionManagementApi.md#listRepositoryTransactions) | **GET** /transactions/{user}/{repo} | List all non expired transactions on the given repository
[**listUserTransactions**](TransactionManagementApi.md#listUserTransactions) | **GET** /transactions/{user} | List all non expired transactions on all the repositories visible to the given user
[**startTransaction**](TransactionManagementApi.md#startTransaction) | **POST** /transactions/{user}/{repo} | Start a new transaction on the given repository


<a name="abortTransaction"></a>
# **abortTransaction**
> TransactionInfo abortTransaction(user, repo, transactionId)

Abort transaction. Returns immediately.

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

var apiInstance = new Geogig.TransactionManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var transactionId = "transactionId_example"; // String | transaction identifier

apiInstance.abortTransaction(user, repo, transactionId).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| repository login owner | 
 **repo** | **String**| repository name | 
 **transactionId** | [**String**](.md)| transaction identifier | 

### Return type

[**TransactionInfo**](TransactionInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="commitTransaction"></a>
# **commitTransaction**
> AsyncTaskInfo commitTransaction(user, repo, transactionId, opts)

Commit transaction. Async operation.

When committing a transaction

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

var apiInstance = new Geogig.TransactionManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var transactionId = "transactionId_example"; // String | transaction identifier

var opts = { 
  'messageTitle': "messageTitle_example", // String | A short (no more than 100 characters) title to summarize the reason this commit is being made
  'messageDescription': "messageDescription_example" // String | A possibly larger, even spanning multiple paragraphs, description of the reason this commit is applied.
};
apiInstance.commitTransaction(user, repo, transactionId, opts).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| repository login owner | 
 **repo** | **String**| repository name | 
 **transactionId** | [**String**](.md)| transaction identifier | 
 **messageTitle** | **String**| A short (no more than 100 characters) title to summarize the reason this commit is being made | [optional] 
 **messageDescription** | **String**| A possibly larger, even spanning multiple paragraphs, description of the reason this commit is applied. | [optional] 

### Return type

[**AsyncTaskInfo**](AsyncTaskInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="deleteTransactionInfo"></a>
# **deleteTransactionInfo**
> deleteTransactionInfo(user, repo, transactionId)

Delete a transaction information that&#39;s finished but not yet expired.

Delete a transaction information. The transaction must be in a finished state (either committed or aborted) for the operation to succeed.

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

var apiInstance = new Geogig.TransactionManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var transactionId = "transactionId_example"; // String | transaction identifier

apiInstance.deleteTransactionInfo(user, repo, transactionId).then(function() {
  console.log('API called successfully.');
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| repository login owner | 
 **repo** | **String**| repository name | 
 **transactionId** | [**String**](.md)| transaction identifier | 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getTransactionInfo"></a>
# **getTransactionInfo**
> TransactionInfo getTransactionInfo(user, repo, transactionId)

Obtain the current status of the given transaction

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

var apiInstance = new Geogig.TransactionManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var transactionId = "transactionId_example"; // String | transaction identifier

apiInstance.getTransactionInfo(user, repo, transactionId).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| repository login owner | 
 **repo** | **String**| repository name | 
 **transactionId** | [**String**](.md)| transaction identifier | 

### Return type

[**TransactionInfo**](TransactionInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listAllTransactions"></a>
# **listAllTransactions**
> [TransactionInfo] listAllTransactions()

List all non expired transactions open an all the repositories visible to the authenticated user

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

var apiInstance = new Geogig.TransactionManagementApi();
apiInstance.listAllTransactions().then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters
This endpoint does not need any parameter.

### Return type

[**[TransactionInfo]**](TransactionInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listRepositoryTransactions"></a>
# **listRepositoryTransactions**
> [TransactionInfo] listRepositoryTransactions(user, repo, )

List all non expired transactions on the given repository

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

var apiInstance = new Geogig.TransactionManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.listRepositoryTransactions(user, repo, ).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| repository login owner | 
 **repo** | **String**| repository name | 

### Return type

[**[TransactionInfo]**](TransactionInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listUserTransactions"></a>
# **listUserTransactions**
> [TransactionInfo] listUserTransactions(user, )

List all non expired transactions on all the repositories visible to the given user

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

var apiInstance = new Geogig.TransactionManagementApi();

var user = "user_example"; // String | repository login owner

apiInstance.listUserTransactions(user, ).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| repository login owner | 

### Return type

[**[TransactionInfo]**](TransactionInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="startTransaction"></a>
# **startTransaction**
> TransactionInfo startTransaction(user, repo, )

Start a new transaction on the given repository

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

var apiInstance = new Geogig.TransactionManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.startTransaction(user, repo, ).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| repository login owner | 
 **repo** | **String**| repository name | 

### Return type

[**TransactionInfo**](TransactionInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

