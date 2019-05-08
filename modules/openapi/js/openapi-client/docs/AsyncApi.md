# Geogig.AsyncApi

All URIs are relative to *http://virtserver.swaggerhub.com/groldan/geogig-web/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**abortTask**](AsyncApi.md#abortTask) | **POST** /tasks/{taskId}/abort | Abort a running or scheduled task
[**commitTransaction**](AsyncApi.md#commitTransaction) | **POST** /transactions/{user}/{repo}/{transactionId}/commit | Commit transaction. Async operation.
[**forkRepository**](AsyncApi.md#forkRepository) | **POST** /repos/{user}/{repo}/forks | Fork this repository. Async operation.
[**getTaskInfo**](AsyncApi.md#getTaskInfo) | **GET** /tasks/{taskId} | Access status info for a given task
[**getTaskProgress**](AsyncApi.md#getTaskProgress) | **GET** /tasks/{taskId}/progress | Get task progress info
[**listTasks**](AsyncApi.md#listTasks) | **GET** /tasks | List summary information for current asynchronous tasks
[**pruneTask**](AsyncApi.md#pruneTask) | **DELETE** /tasks/{taskId} | Prune a task if finished


<a name="abortTask"></a>
# **abortTask**
> AsyncTaskInfo abortTask(taskId)

Abort a running or scheduled task

Abort a running or scheduled task. If the task is already finished, the operation has no effect and its current state is returned. If the tasks is not finished, the returned task info status might be either ABORTING or ABORTED

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

var apiInstance = new Geogig.AsyncApi();

var taskId = "taskId_example"; // String | Async task identifier

apiInstance.abortTask(taskId).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **taskId** | [**String**](.md)| Async task identifier | 

### Return type

[**AsyncTaskInfo**](AsyncTaskInfo.md)

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

var apiInstance = new Geogig.AsyncApi();

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

var apiInstance = new Geogig.AsyncApi();

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

<a name="getTaskInfo"></a>
# **getTaskInfo**
> AsyncTaskInfo getTaskInfo(taskId, opts)

Access status info for a given task

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

var apiInstance = new Geogig.AsyncApi();

var taskId = "taskId_example"; // String | Async task identifier

var opts = { 
  'prune': true // Boolean | If provided and true, the task information is pruned (deleted) if the operation is complete (either successfully or not)
};
apiInstance.getTaskInfo(taskId, opts).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **taskId** | [**String**](.md)| Async task identifier | 
 **prune** | **Boolean**| If provided and true, the task information is pruned (deleted) if the operation is complete (either successfully or not) | [optional] 

### Return type

[**AsyncTaskInfo**](AsyncTaskInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getTaskProgress"></a>
# **getTaskProgress**
> ProgressInfo getTaskProgress(taskId)

Get task progress info

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

var apiInstance = new Geogig.AsyncApi();

var taskId = "taskId_example"; // String | Async task identifier

apiInstance.getTaskProgress(taskId).then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **taskId** | [**String**](.md)| Async task identifier | 

### Return type

[**ProgressInfo**](ProgressInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listTasks"></a>
# **listTasks**
> [AsyncTaskInfo] listTasks()

List summary information for current asynchronous tasks

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

var apiInstance = new Geogig.AsyncApi();
apiInstance.listTasks().then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters
This endpoint does not need any parameter.

### Return type

[**[AsyncTaskInfo]**](AsyncTaskInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="pruneTask"></a>
# **pruneTask**
> pruneTask(taskId)

Prune a task if finished

If the task is finished, then deletes its information, fail otherwise

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

var apiInstance = new Geogig.AsyncApi();

var taskId = "taskId_example"; // String | Async task identifier

apiInstance.pruneTask(taskId).then(function() {
  console.log('API called successfully.');
}, function(error) {
  console.error(error);
});

```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **taskId** | [**String**](.md)| Async task identifier | 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

