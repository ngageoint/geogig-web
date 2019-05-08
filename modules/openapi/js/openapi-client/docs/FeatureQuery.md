# Geogig.FeatureQuery

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**head** | **String** | tree-ish resolving to the root tree to query. Defaults to WORK_HEAD | [optional] [default to &#39;WORK_HEAD&#39;]
**attributes** | **[String]** |  | [optional] 
**filter** | [**FeatureFilter**](FeatureFilter.md) |  | [optional] 
**resultType** | **String** |  | [optional] 
**outputCrs** | [**SRS**](SRS.md) |  | [optional] 
**offset** | **Number** |  | [optional] 
**limit** | **Number** |  | [optional] 
**precision** | **Number** | Multiplying factor used to obtain a precise ordinate. For example, in order to round ordinates to 3 significant digits use a value of 1000. A value absent or 0 (zero) means to not apply any rounding on ordinates. The maximum allowed value is 1,000,000,000 for a precision of 9 decimal places, which is the tolerance used by GeoGig to compare coordinate equality | [optional] [default to 0]
**simplificationDistance** | **Number** | Perform non topology preserving geometry generalization with the given tolerance (may return self crossing polygons as result of the generalization) | [optional] 


<a name="ResultTypeEnum"></a>
## Enum: ResultTypeEnum


* `FEATURES` (value: `"FEATURES"`)

* `BOUNDS` (value: `"BOUNDS"`)

* `COUNT` (value: `"COUNT"`)

* `FIDS` (value: `"FIDS"`)




