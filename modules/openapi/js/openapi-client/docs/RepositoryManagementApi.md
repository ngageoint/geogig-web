# Geogig.RepositoryManagementApi

All URIs are relative to *http://virtserver.swaggerhub.com/groldan/geogig-web/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**countWatchers**](RepositoryManagementApi.md#countWatchers) | **GET** /repos/{user}/{repo}/watchers/count | Number of users watching this repository
[**createRepository**](RepositoryManagementApi.md#createRepository) | **POST** /repos/{user}/{repo} | Create a new repository
[**deleteRepository**](RepositoryManagementApi.md#deleteRepository) | **DELETE** /repos/{user}/{repo} | Delete repository
[**forkRepository**](RepositoryManagementApi.md#forkRepository) | **POST** /repos/{user}/{repo}/forks | Fork this repository. Async operation.
[**getRepository**](RepositoryManagementApi.md#getRepository) | **GET** /repos/{user}/{repo} | Get full repository information
[**listForks**](RepositoryManagementApi.md#listForks) | **GET** /repos/{user}/{repo}/forks | List repositories that are forks of this one
[**listRepositories**](RepositoryManagementApi.md#listRepositories) | **GET** /repos | List repositories summary information
[**listUserRepositories**](RepositoryManagementApi.md#listUserRepositories) | **GET** /repos/{user} | List of available repositories summary information
[**listWatchers**](RepositoryManagementApi.md#listWatchers) | **GET** /repos/{user}/{repo}/watchers | List users watching this repository
[**modifyRepository**](RepositoryManagementApi.md#modifyRepository) | **PUT** /repos/{user}/{repo} | Modify repository
[**unwatchRepository**](RepositoryManagementApi.md#unwatchRepository) | **DELETE** /repos/{user}/{repo}/watchers | Stop watching this repo.
[**watchRepository**](RepositoryManagementApi.md#watchRepository) | **POST** /repos/{user}/{repo}/watchers | Start watching this repo.


<a name="countWatchers"></a>
# **countWatchers**
> &#39;Number&#39; countWatchers(user, repo, )

Number of users watching this repository

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.countWatchers(user, repo, ).then(function(data) {
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

**&#39;Number&#39;**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="createRepository"></a>
# **createRepository**
> RepositoryInfo createRepository(user, repo, , opts)

Create a new repository

Create a new repository

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'targetStore': "targetStore_example" // String | Optionally, the name of the target store where to save the repo. Defaults to the owner's default store.
};
apiInstance.createRepository(user, repo, , opts).then(function(data) {
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
 **targetStore** | **String**| Optionally, the name of the target store where to save the repo. Defaults to the owner&#39;s default store. | [optional] 

### Return type

[**RepositoryInfo**](RepositoryInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="deleteRepository"></a>
# **deleteRepository**
> deleteRepository(user, repo, )

Delete repository

Deletes the repository addressed by the request path

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.deleteRepository(user, repo, ).then(function() {
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

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="forkRepository"></a>
# **forkRepository**
> AsyncTaskInfo forkRepository(user, repo, , opts)

Fork this repository. Async operation.

Forks this repository asynchronously to the authenticated user&#39;s account. Forking is the same than cloning but the term is used in this context to differentiate cloning to any allowed remote URI vs creating clone of this repository inside this server.

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'forkName': "forkName_example", // String | Optional name for the forked repository under the authenticated user's account. If not provided defaults to the source repository name
  'targetStore': "targetStore_example" // String | Name of the repository store writable to the authenticated user to fork this repo to. If not provided, the current user's default store is used.
};
apiInstance.forkRepository(user, repo, , opts).then(function(data) {
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
 **forkName** | **String**| Optional name for the forked repository under the authenticated user&#39;s account. If not provided defaults to the source repository name | [optional] 
 **targetStore** | **String**| Name of the repository store writable to the authenticated user to fork this repo to. If not provided, the current user&#39;s default store is used. | [optional] 

### Return type

[**AsyncTaskInfo**](AsyncTaskInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getRepository"></a>
# **getRepository**
> RepositoryInfo getRepository(user, repo, )

Get full repository information

Obtain a manifest about the current status of the repository

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.getRepository(user, repo, ).then(function(data) {
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

[**RepositoryInfo**](RepositoryInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listForks"></a>
# **listForks**
> RepositoryInfo listForks(user, repo, )

List repositories that are forks of this one

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.listForks(user, repo, ).then(function(data) {
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

[**RepositoryInfo**](RepositoryInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listRepositories"></a>
# **listRepositories**
> [RepositoryInfo] listRepositories(opts)

List repositories summary information

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

var apiInstance = new Geogig.RepositoryManagementApi();

var opts = { 
  'topics': ["topics_example"] // [String] | 
};
apiInstance.listRepositories(opts).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **topics** | [**[String]**](String.md)|  | [optional] 

### Return type

[**[RepositoryInfo]**](RepositoryInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listUserRepositories"></a>
# **listUserRepositories**
> [RepositoryInfo] listUserRepositories(user, )

List of available repositories summary information

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

apiInstance.listUserRepositories(user, ).then(function(data) {
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

[**[RepositoryInfo]**](RepositoryInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listWatchers"></a>
# **listWatchers**
> UserInfo listWatchers(user, repo, )

List users watching this repository

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.listWatchers(user, repo, ).then(function(data) {
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

[**UserInfo**](UserInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="modifyRepository"></a>
# **modifyRepository**
> RepositoryInfo modifyRepository(user, repo, repository)

Modify repository

Modify repository

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var repository = new Geogig.RepositoryInfo(); // RepositoryInfo | The new repository settings

apiInstance.modifyRepository(user, repo, repository).then(function(data) {
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
 **repository** | [**RepositoryInfo**](RepositoryInfo.md)| The new repository settings | 

### Return type

[**RepositoryInfo**](RepositoryInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/x-jackson-smile

<a name="unwatchRepository"></a>
# **unwatchRepository**
> unwatchRepository(user, repo, )

Stop watching this repo.

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.unwatchRepository(user, repo, ).then(function() {
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

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="watchRepository"></a>
# **watchRepository**
> watchRepository(user, repo, )

Start watching this repo.

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

var apiInstance = new Geogig.RepositoryManagementApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

apiInstance.watchRepository(user, repo, ).then(function() {
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

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

