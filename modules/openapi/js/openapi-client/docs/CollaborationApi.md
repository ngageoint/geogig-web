# Geogig.CollaborationApi

All URIs are relative to *http://virtserver.swaggerhub.com/groldan/geogig-web/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**checkout**](CollaborationApi.md#checkout) | **POST** /repos/{user}/{repo}/branches/{branch}/checkout | Checkout this branch
[**createBranch**](CollaborationApi.md#createBranch) | **POST** /repos/{user}/{repo}/branches/{branch} | Create a new branch by name
[**createPullRequest**](CollaborationApi.md#createPullRequest) | **POST** /repos/{user}/{repo}/pulls | Create a pull request
[**deleteBranch**](CollaborationApi.md#deleteBranch) | **DELETE** /repos/{user}/{repo}/branches/{branch} | Delete a branch by name
[**getBranch**](CollaborationApi.md#getBranch) | **GET** /repos/{user}/{repo}/branches/{branch} | Get a branch by name
[**getCurrentBranch**](CollaborationApi.md#getCurrentBranch) | **GET** /repos/{user}/{repo}/branch | Get the currently checked out branch, optionally indicating a transaction
[**getPullRequest**](CollaborationApi.md#getPullRequest) | **GET** /repos/{user}/{repo}/pulls/{pr} | Get a specific pull request of this repo by id
[**isPullRequestMerged**](CollaborationApi.md#isPullRequestMerged) | **GET** /repos/{user}/{repo}/pulls/{pr}/merge | Check on the merged status of the pull request
[**listBranches**](CollaborationApi.md#listBranches) | **GET** /repos/{user}/{repo}/branches | List all branches in the current repository
[**listPullRequests**](CollaborationApi.md#listPullRequests) | **GET** /repos/{user}/{repo}/pulls | List pull requests issued to this repo
[**mergePullRequest**](CollaborationApi.md#mergePullRequest) | **PUT** /repos/{user}/{repo}/pulls/{pr}/merge | Merge the pull request
[**updatePullRequest**](CollaborationApi.md#updatePullRequest) | **PATCH** /repos/{user}/{repo}/pulls/{pr} | Change the title, description, open status, or target branch of the pull request


<a name="checkout"></a>
# **checkout**
> BranchInfo checkout(user, repo, branch, geogigTransactionId, opts)

Checkout this branch

Checkout this branch, making it the current branch in the repository

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var branch = "branch_example"; // String | branch name

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

var opts = { 
  'force': false // Boolean | 
};
apiInstance.checkout(user, repo, branch, geogigTransactionId, opts).then(function(data) {
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
 **branch** | **String**| branch name | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 
 **force** | **Boolean**|  | [optional] [default to false]

### Return type

[**BranchInfo**](BranchInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="createBranch"></a>
# **createBranch**
> BranchInfo createBranch(user, repo, branch, commitish, opts)

Create a new branch by name

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var branch = "branch_example"; // String | branch name

var commitish = "commitish_example"; // String | Origin commit-ish string where to create the branch from. Can be another branche's name, a commit sha id, or any other string that rev-parse resolves to a commit (e.g. HEAD~2, refs/heads/branchname, etc.)

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
  'description': "description_example" // String | Optional short description specifying the purpose of the branch
};
apiInstance.createBranch(user, repo, branch, commitish, opts).then(function(data) {
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
 **branch** | **String**| branch name | 
 **commitish** | **String**| Origin commit-ish string where to create the branch from. Can be another branche&#39;s name, a commit sha id, or any other string that rev-parse resolves to a commit (e.g. HEAD~2, refs/heads/branchname, etc.) | 
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 
 **description** | **String**| Optional short description specifying the purpose of the branch | [optional] 

### Return type

[**BranchInfo**](BranchInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="createPullRequest"></a>
# **createPullRequest**
> PullRequestInfo createPullRequest(user, repo, pullRequestRequest)

Create a pull request

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var pullRequestRequest = new Geogig.PullRequestRequest(); // PullRequestRequest | 

apiInstance.createPullRequest(user, repo, pullRequestRequest).then(function(data) {
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
 **pullRequestRequest** | [**PullRequestRequest**](PullRequestRequest.md)|  | 

### Return type

[**PullRequestInfo**](PullRequestInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="deleteBranch"></a>
# **deleteBranch**
> deleteBranch(user, repo, branch, , opts)

Delete a branch by name

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var branch = "branch_example"; // String | branch name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.deleteBranch(user, repo, branch, , opts).then(function() {
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
 **branch** | **String**| branch name | 
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getBranch"></a>
# **getBranch**
> BranchInfo getBranch(user, repo, branch, , opts)

Get a branch by name

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var branch = "branch_example"; // String | branch name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getBranch(user, repo, branch, , opts).then(function(data) {
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
 **branch** | **String**| branch name | 
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

[**BranchInfo**](BranchInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getCurrentBranch"></a>
# **getCurrentBranch**
> BranchInfo getCurrentBranch(user, repo, , opts)

Get the currently checked out branch, optionally indicating a transaction

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getCurrentBranch(user, repo, , opts).then(function(data) {
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

[**BranchInfo**](BranchInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getPullRequest"></a>
# **getPullRequest**
> PullRequestInfo getPullRequest(user, repo, pr)

Get a specific pull request of this repo by id

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var pr = 56; // Number | Pull request identifier

apiInstance.getPullRequest(user, repo, pr).then(function(data) {
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
 **pr** | **Number**| Pull request identifier | 

### Return type

[**PullRequestInfo**](PullRequestInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="isPullRequestMerged"></a>
# **isPullRequestMerged**
> &#39;Boolean&#39; isPullRequestMerged(user, repo, pr)

Check on the merged status of the pull request

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var pr = 56; // Number | Pull request identifier

apiInstance.isPullRequestMerged(user, repo, pr).then(function(data) {
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
 **pr** | **Number**| Pull request identifier | 

### Return type

**&#39;Boolean&#39;**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listBranches"></a>
# **listBranches**
> [BranchInfo] listBranches(user, repo, , opts)

List all branches in the current repository

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.listBranches(user, repo, , opts).then(function(data) {
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

[**[BranchInfo]**](BranchInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listPullRequests"></a>
# **listPullRequests**
> [PullRequestInfo] listPullRequests(user, repo, , opts)

List pull requests issued to this repo

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'open': true, // Boolean | 
  'closed': false // Boolean | 
};
apiInstance.listPullRequests(user, repo, , opts).then(function(data) {
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
 **open** | **Boolean**|  | [optional] [default to true]
 **closed** | **Boolean**|  | [optional] [default to false]

### Return type

[**[PullRequestInfo]**](PullRequestInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="mergePullRequest"></a>
# **mergePullRequest**
> ObjectHash mergePullRequest(user, repo, pr, opts)

Merge the pull request

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var pr = 56; // Number | Pull request identifier

var opts = { 
  'commitTitle': "commitTitle_example", // String | Short, single line, title for the automatic commit message
  'commitMessage': "commitMessage_example" // String | Extra detail to append to automatic commit message, possibly encompassing multiple paragraphs
};
apiInstance.mergePullRequest(user, repo, pr, opts).then(function(data) {
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
 **pr** | **Number**| Pull request identifier | 
 **commitTitle** | **String**| Short, single line, title for the automatic commit message | [optional] 
 **commitMessage** | **String**| Extra detail to append to automatic commit message, possibly encompassing multiple paragraphs | [optional] 

### Return type

[**ObjectHash**](ObjectHash.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="updatePullRequest"></a>
# **updatePullRequest**
> PullRequestInfo updatePullRequest(user, repo, prrequestRequestPatch)

Change the title, description, open status, or target branch of the pull request

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

var apiInstance = new Geogig.CollaborationApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var pr = 56; // Number | Pull request identifier

var requestRequestPatch = new Geogig.RequestRequestPatch(); // RequestRequestPatch | 

apiInstance.updatePullRequest(user, repo, prrequestRequestPatch).then(function(data) {
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
 **pr** | **Number**| Pull request identifier | 
 **requestRequestPatch** | [**RequestRequestPatch**](RequestRequestPatch.md)|  | 

### Return type

[**PullRequestInfo**](PullRequestInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

