# Geogig.RawRepositoryAccessApi

All URIs are relative to *http://virtserver.swaggerhub.com/groldan/geogig-web/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**countConflicts**](RawRepositoryAccessApi.md#countConflicts) | **GET** /repos/{user}/{repo}/geogig/conflicts/count | Get number of conflicts in the repository
[**getAllConfig**](RawRepositoryAccessApi.md#getAllConfig) | **GET** /repos/{user}/{repo}/geogig/config | List all config settings in the current repository
[**getCommitGraph**](RawRepositoryAccessApi.md#getCommitGraph) | **GET** /repos/{user}/{repo}/geogig/graph | 
[**getConflicts**](RawRepositoryAccessApi.md#getConflicts) | **GET** /repos/{user}/{repo}/geogig/conflicts | List merge conflicts
[**getObject**](RawRepositoryAccessApi.md#getObject) | **GET** /repos/{user}/{repo}/geogig/objects/{objectId} | Download a single revision object from a repository
[**listIndexes**](RawRepositoryAccessApi.md#listIndexes) | **GET** /repos/{user}/{repo}/geogig/index | 
[**listRefs**](RawRepositoryAccessApi.md#listRefs) | **GET** /repos/{user}/{repo}/geogig/refs | List all refs in the current repository


<a name="countConflicts"></a>
# **countConflicts**
> &#39;Number&#39; countConflicts(user, repo, , opts)

Get number of conflicts in the repository



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

var apiInstance = new Geogig.RawRepositoryAccessApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.countConflicts(user, repo, , opts).then(function(data) {
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
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

**&#39;Number&#39;**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getAllConfig"></a>
# **getAllConfig**
> {&#39;String&#39;: &#39;String&#39;} getAllConfig(user, repo, , opts)

List all config settings in the current repository

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

var apiInstance = new Geogig.RawRepositoryAccessApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getAllConfig(user, repo, , opts).then(function(data) {
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
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

**{&#39;String&#39;: &#39;String&#39;}**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getCommitGraph"></a>
# **getCommitGraph**
> &#39;Number&#39; getCommitGraph(user, repo, , opts)





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

var apiInstance = new Geogig.RawRepositoryAccessApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getCommitGraph(user, repo, , opts).then(function(data) {
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
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

**&#39;Number&#39;**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getConflicts"></a>
# **getConflicts**
> RefMap getConflicts(user, repo, , opts)

List merge conflicts



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

var apiInstance = new Geogig.RawRepositoryAccessApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getConflicts(user, repo, , opts).then(function(data) {
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
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

[**RefMap**](RefMap.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getObject"></a>
# **getObject**
> RevisionObject getObject(user, repo, objectId)

Download a single revision object from a repository

Download a single revision object from a repository

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

var apiInstance = new Geogig.RawRepositoryAccessApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var objectId = "objectId_example"; // String | SHA1 hash in hex format of the object to retrieve

apiInstance.getObject(user, repo, objectId).then(function(data) {
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
 **objectId** | **String**| SHA1 hash in hex format of the object to retrieve | 

### Return type

[**RevisionObject**](RevisionObject.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile, text/plain

<a name="listIndexes"></a>
# **listIndexes**
> &#39;Number&#39; listIndexes(user, repo, , opts)





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

var apiInstance = new Geogig.RawRepositoryAccessApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.listIndexes(user, repo, , opts).then(function(data) {
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
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

**&#39;Number&#39;**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listRefs"></a>
# **listRefs**
> RefMap listRefs(user, repo, , opts)

List all refs in the current repository

List all refs in the current repository. Refs in the /transactions namespace are ommitted. Instead, if the geogig-transaction-id header parameter is provided, the refs under that transaction are returned.

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

var apiInstance = new Geogig.RawRepositoryAccessApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.listRefs(user, repo, , opts).then(function(data) {
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
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

[**RefMap**](RefMap.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

