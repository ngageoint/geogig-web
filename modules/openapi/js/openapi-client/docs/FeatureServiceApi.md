# Geogig.FeatureServiceApi

All URIs are relative to *http://virtserver.swaggerhub.com/groldan/geogig-web/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addFeatures**](FeatureServiceApi.md#addFeatures) | **POST** /layers/{user}/{repo}/{layer}/features | Iserts features to this collection in the WORK_HEAD. Parameter features can be either a Feature or a FeatureCollection.
[**createLayer**](FeatureServiceApi.md#createLayer) | **POST** /layers/{user}/{repo} | Create a new layer
[**deleteFeature**](FeatureServiceApi.md#deleteFeature) | **DELETE** /layers/{user}/{repo}/{layer}/features/{featureId} | Delete a single feature at the current transaction&#39;s branch
[**deleteFeatures**](FeatureServiceApi.md#deleteFeatures) | **POST** /layers/{user}/{repo}/{layer}/rpc/delete | Delete features matching the given query
[**deleteLayer**](FeatureServiceApi.md#deleteLayer) | **DELETE** /layers/{user}/{repo}/{layer} | Delete a Layer from a repository at the current transaction&#39;s branch
[**getBounds**](FeatureServiceApi.md#getBounds) | **GET** /layers/{user}/{repo}/{layer}/bounds | Get layer bounds at the specified transaction/head. If no head is specified, defaults to WORK_HEAD
[**getFeature**](FeatureServiceApi.md#getFeature) | **GET** /layers/{user}/{repo}/{layer}/features/{featureId} | Get a single feature from a layer given its feature id, at the specified transaction/head
[**getFeatures**](FeatureServiceApi.md#getFeatures) | **GET** /layers/{user}/{repo}/{layer}/features | Get Features from a geogig Layer
[**getLayerHash**](FeatureServiceApi.md#getLayerHash) | **HEAD** /layers/{user}/{repo}/{layer} | Get a unique identifier for the current state of the layer.
[**getLayerInfo**](FeatureServiceApi.md#getLayerInfo) | **GET** /layers/{user}/{repo}/{layer} | Get layer metadata at the specified transaction/head
[**getLayersSummaries**](FeatureServiceApi.md#getLayersSummaries) | **GET** /layers | return a summary of layers per user
[**getSchema**](FeatureServiceApi.md#getSchema) | **GET** /layers/{user}/{repo}/{layer}/schema | Get Layer schema
[**getSize**](FeatureServiceApi.md#getSize) | **GET** /layers/{user}/{repo}/{layer}/size | Get layer&#39;s number of features at the specified transaction/head. If no head is specified, defaults to WORK_HEAD
[**getUserLayersSummaries**](FeatureServiceApi.md#getUserLayersSummaries) | **GET** /layers/{user} | return a summary of this user&#39;s layers
[**listLayers**](FeatureServiceApi.md#listLayers) | **GET** /layers/{user}/{repo} | List layers in a repo
[**modifyFeature**](FeatureServiceApi.md#modifyFeature) | **PUT** /layers/{user}/{repo}/{layer}/features/{featureId} | Replace a single feature at the current transaction&#39;s branch
[**modifyFeatures**](FeatureServiceApi.md#modifyFeatures) | **POST** /layers/{user}/{repo}/{layer}/rpc/update | Modifies features matching the given query with the given values, in the WORK_HEAD of the currently checked out branch for the indicated transaction
[**queryFeatures**](FeatureServiceApi.md#queryFeatures) | **POST** /layers/{user}/{repo}/{layer}/rpc/query | Query and return features of this layer tha match the specified FeatureQuery
[**truncate**](FeatureServiceApi.md#truncate) | **DELETE** /layers/{user}/{repo}/{layer}/features | Deletes all features in the layer
[**updateSchema**](FeatureServiceApi.md#updateSchema) | **PUT** /layers/{user}/{repo}/{layer}/schema | Change Layer schema. Must be called inside a transaction.


<a name="addFeatures"></a>
# **addFeatures**
> addFeatures(user, repo, layer, geogigTransactionId, opts)

Iserts features to this collection in the WORK_HEAD. Parameter features can be either a Feature or a FeatureCollection.

The features are added to the WORK_HEAD tree. This operation works against the currently checked out branch for the indicated transaction, and does not perform a geogig commit on its own. The geogig commit is created either manually or once the geogig transaction is committed.

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

var opts = { 
  'features': new Geogig.Feature() // Feature | 
};
apiInstance.addFeatures(user, repo, layer, geogigTransactionId, opts).then(function() {
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
 **layer** | **String**| layer name for feature service | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 
 **features** | [**Feature**](Feature.md)|  | [optional] 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: application/vnd.geo+json, application/vnd.geo+smile
 - **Accept**: application/json, application/x-jackson-smile

<a name="createLayer"></a>
# **createLayer**
> LayerInfo createLayer(user, repo, geogigTransactionIdfeatureType)

Create a new layer

Creating and populating a new layer are two separate steps. First the layer schema must be created and then the features uploaded.

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

var featureType = new Geogig.RevisionFeatureType(); // RevisionFeatureType | FeatureType for the layer

apiInstance.createLayer(user, repo, geogigTransactionIdfeatureType).then(function(data) {
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
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 
 **featureType** | [**RevisionFeatureType**](RevisionFeatureType.md)| FeatureType for the layer | 

### Return type

[**LayerInfo**](LayerInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="deleteFeature"></a>
# **deleteFeature**
> deleteFeature(user, repo, layer, featureId, geogigTransactionId)

Delete a single feature at the current transaction&#39;s branch

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var featureId = "featureId_example"; // String | Feature identifier

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

apiInstance.deleteFeature(user, repo, layer, featureId, geogigTransactionId).then(function() {
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
 **layer** | **String**| layer name for feature service | 
 **featureId** | **String**| Feature identifier | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="deleteFeatures"></a>
# **deleteFeatures**
> deleteFeatures(user, repo, layer, geogigTransactionId, opts)

Delete features matching the given query

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

var opts = { 
  'filter': new Geogig.FeatureFilter() // FeatureFilter | 
};
apiInstance.deleteFeatures(user, repo, layer, geogigTransactionId, opts).then(function() {
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
 **layer** | **String**| layer name for feature service | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 
 **filter** | [**FeatureFilter**](FeatureFilter.md)|  | [optional] 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="deleteLayer"></a>
# **deleteLayer**
> deleteLayer(user, repo, layer, geogigTransactionId)

Delete a Layer from a repository at the current transaction&#39;s branch

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

apiInstance.deleteLayer(user, repo, layer, geogigTransactionId).then(function() {
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
 **layer** | **String**| layer name for feature service | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getBounds"></a>
# **getBounds**
> BoundingBox getBounds(user, repo, layer, , opts)

Get layer bounds at the specified transaction/head. If no head is specified, defaults to WORK_HEAD

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var opts = { 
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getBounds(user, repo, layer, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

[**BoundingBox**](BoundingBox.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getFeature"></a>
# **getFeature**
> Feature getFeature(user, repo, layer, featureId, , opts)

Get a single feature from a layer given its feature id, at the specified transaction/head

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var featureId = "featureId_example"; // String | Feature identifier

var opts = { 
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
  'ifNoneMatch': "ifNoneMatch_example" // String | Conditional get, return feature its revision feature hash does not match the provided hash
};
apiInstance.getFeature(user, repo, layer, featureId, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **featureId** | **String**| Feature identifier | 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 
 **ifNoneMatch** | **String**| Conditional get, return feature its revision feature hash does not match the provided hash | [optional] 

### Return type

[**Feature**](Feature.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/vnd.geo+json, application/vnd.geo+smile

<a name="getFeatures"></a>
# **getFeatures**
> FeatureCollection getFeatures(user, repo, layer, , opts)

Get Features from a geogig Layer

Obtain a stream of features from a geogig layer at the specified transaction/head

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var opts = { 
  'bbox': [3.4], // [Number] | Comma separated values for a query bounding box in minx,miny,maxx,maxy format. Example: ?bbox=-180,-90,180,90
  'attributes': ["attributes_example"], // [String] | Ordered list of attribute names to return
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
  'page': 56, // Number | Page number to retrive when requesting collections, starts at 1.
  'pageSize': 56, // Number | number of elements to return in a paging query. If page is specified and pageSize is not, pageSize defaults to 100. If pageSize is specified, page becomes mandatory
  'ifNoneMatch': "ifNoneMatch_example" // String | Conditional get, return features if the layer tree does not match the provided hash
};
apiInstance.getFeatures(user, repo, layer, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **bbox** | [**[Number]**](Number.md)| Comma separated values for a query bounding box in minx,miny,maxx,maxy format. Example: ?bbox&#x3D;-180,-90,180,90 | [optional] 
 **attributes** | [**[String]**](String.md)| Ordered list of attribute names to return | [optional] 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 
 **page** | **Number**| Page number to retrive when requesting collections, starts at 1. | [optional] 
 **pageSize** | **Number**| number of elements to return in a paging query. If page is specified and pageSize is not, pageSize defaults to 100. If pageSize is specified, page becomes mandatory | [optional] 
 **ifNoneMatch** | **String**| Conditional get, return features if the layer tree does not match the provided hash | [optional] 

### Return type

[**FeatureCollection**](FeatureCollection.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/vnd.geo+json, application/vnd.geo+smile, application/vnd.geogig.features+json, application/vnd.geogig.features+smile

<a name="getLayerHash"></a>
# **getLayerHash**
> ObjectHash getLayerHash(user, repo, layer, , opts)

Get a unique identifier for the current state of the layer.

Get a unique identifier for the current state of the layer. Any change/edit to the layer will change this identifier. This operation can be used to verify whether the layer has changed since the client last accessed it.

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var opts = { 
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getLayerHash(user, repo, layer, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

[**ObjectHash**](ObjectHash.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

<a name="getLayerInfo"></a>
# **getLayerInfo**
> LayerInfo getLayerInfo(user, repo, layer, , opts)

Get layer metadata at the specified transaction/head

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var opts = { 
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
  'ifNoneMatch': "ifNoneMatch_example" // String | Conditional get, layer info if its revision tree hash does not match the provided hash
};
apiInstance.getLayerInfo(user, repo, layer, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 
 **ifNoneMatch** | **String**| Conditional get, layer info if its revision tree hash does not match the provided hash | [optional] 

### Return type

[**LayerInfo**](LayerInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getLayersSummaries"></a>
# **getLayersSummaries**
> [UserLayerSummary] getLayersSummaries()

return a summary of layers per user

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

var apiInstance = new Geogig.FeatureServiceApi();
apiInstance.getLayersSummaries().then(function(data) {
  console.log('API called successfully. Returned data: ' + data);
}, function(error) {
  console.error(error);
});

```

### Parameters
This endpoint does not need any parameter.

### Return type

[**[UserLayerSummary]**](UserLayerSummary.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getSchema"></a>
# **getSchema**
> RevisionFeatureType getSchema(user, repo, layer, , opts)

Get Layer schema

Get layer&#39;s Feature Type at the specified transaction/head. If no head is specified, defaults to WORK_HEAD

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var opts = { 
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getSchema(user, repo, layer, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

[**RevisionFeatureType**](RevisionFeatureType.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getSize"></a>
# **getSize**
> &#39;Number&#39; getSize(user, repo, layer, , opts)

Get layer&#39;s number of features at the specified transaction/head. If no head is specified, defaults to WORK_HEAD

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var opts = { 
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.getSize(user, repo, layer, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

**&#39;Number&#39;**

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="getUserLayersSummaries"></a>
# **getUserLayersSummaries**
> [RepoLayerSummary] getUserLayersSummaries(user, )

return a summary of this user&#39;s layers

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

apiInstance.getUserLayersSummaries(user, ).then(function(data) {
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

[**[RepoLayerSummary]**](RepoLayerSummary.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="listLayers"></a>
# **listLayers**
> [LayerInfo] listLayers(user, repo, , opts)

List layers in a repo

List the available layers in the requested [transaction]/repository/[head]

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var opts = { 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
};
apiInstance.listLayers(user, repo, , opts).then(function(data) {
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
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]

### Return type

[**[LayerInfo]**](LayerInfo.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="modifyFeature"></a>
# **modifyFeature**
> modifyFeature(user, repo, layer, featureId, geogigTransactionId)

Replace a single feature at the current transaction&#39;s branch

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var featureId = "featureId_example"; // String | Feature identifier

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

apiInstance.modifyFeature(user, repo, layer, featureId, geogigTransactionId).then(function() {
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
 **layer** | **String**| layer name for feature service | 
 **featureId** | **String**| Feature identifier | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: application/vnd.geo+json
 - **Accept**: application/json, application/x-jackson-smile

<a name="modifyFeatures"></a>
# **modifyFeatures**
> modifyFeatures(user, repo, layer, geogigTransactionId, opts)

Modifies features matching the given query with the given values, in the WORK_HEAD of the currently checked out branch for the indicated transaction

The features are looked up and modifies in current  WORK_HEAD tree. This operation works against the currently checked out branch for the indicated transaction, and does not perform a geogig commit on its own. The geogig commit is created either manually or once the geogig transaction is committed.

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

var opts = { 
  'query': new Geogig.UpdateFeaturesRequest() // UpdateFeaturesRequest | 
};
apiInstance.modifyFeatures(user, repo, layer, geogigTransactionId, opts).then(function() {
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
 **layer** | **String**| layer name for feature service | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 
 **query** | [**UpdateFeaturesRequest**](UpdateFeaturesRequest.md)|  | [optional] 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="queryFeatures"></a>
# **queryFeatures**
> FeatureCollection queryFeatures(user, repo, layer, , opts)

Query and return features of this layer tha match the specified FeatureQuery

The features are looked up WORK_HEAD tree unless the head parameter has been provided.

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var opts = { 
  'query': new Geogig.FeatureQuery(), // FeatureQuery | 
  'geogigTransactionId': "geogigTransactionId_example", // String | Optional transaction identifier
};
apiInstance.queryFeatures(user, repo, layer, , opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **query** | [**FeatureQuery**](FeatureQuery.md)|  | [optional] 
 **geogigTransactionId** | [**String**](.md)| Optional transaction identifier | [optional] 

### Return type

[**FeatureCollection**](FeatureCollection.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/vnd.geo+json, application/vnd.geo+smile, application/vnd.geogig.features+json, application/vnd.geogig.features+smile

<a name="truncate"></a>
# **truncate**
> truncate(user, repo, layer, geogigTransactionId)

Deletes all features in the layer

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

apiInstance.truncate(user, repo, layer, geogigTransactionId).then(function() {
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
 **layer** | **String**| layer name for feature service | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 

### Return type

null (empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/x-jackson-smile

<a name="updateSchema"></a>
# **updateSchema**
> RevisionFeatureType updateSchema(user, repo, layer, geogigTransactionIdfeatureType, opts)

Change Layer schema. Must be called inside a transaction.

Modify the layer&#39;s number of features at the specified transaction/head. If no head is specified, defaults to WORK_HEAD

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

var apiInstance = new Geogig.FeatureServiceApi();

var user = "user_example"; // String | repository login owner

var repo = "repo_example"; // String | repository name

var layer = "layer_example"; // String | layer name for feature service

var geogigTransactionId = "geogigTransactionId_example"; // String | transaction identifier, required

var featureType = new Geogig.RevisionFeatureType(); // RevisionFeatureType | FeatureType for the layer

var opts = { 
  'head': "WORK_HEAD", // String | a refSpec string leading to a root RevTree
};
apiInstance.updateSchema(user, repo, layer, geogigTransactionIdfeatureType, opts).then(function(data) {
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
 **layer** | **String**| layer name for feature service | 
 **geogigTransactionId** | [**String**](.md)| transaction identifier, required | 
 **featureType** | [**RevisionFeatureType**](RevisionFeatureType.md)| FeatureType for the layer | 
 **head** | **String**| a refSpec string leading to a root RevTree | [optional] [default to WORK_HEAD]

### Return type

[**RevisionFeatureType**](RevisionFeatureType.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth), [BasicAuth](../README.md#BasicAuth), [OAuth2](../README.md#OAuth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/x-jackson-smile

