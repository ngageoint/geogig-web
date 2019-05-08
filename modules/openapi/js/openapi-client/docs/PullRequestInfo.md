# Geogig.PullRequestInfo

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **Number** |  | [optional] 
**sourceBranch** | **String** |  | [optional] 
**targetBranch** | **String** |  | [optional] 
**title** | **String** |  | [optional] 
**description** | **String** |  | [optional] 
**status** | **String** |  | [optional] 
**mergeable** | **String** |  | [optional] 
**mergeCommit** | **String** |  | [optional] 
**createdAt** | **Date** |  | [optional] 
**updatedAt** | **Date** |  | [optional] 
**closedAt** | **Date** |  | [optional] 
**createdBy** | [**UserInfo**](UserInfo.md) |  | [optional] 
**closedBy** | [**UserInfo**](UserInfo.md) |  | [optional] 
**sourceRepo** | [**RepositoryInfo**](RepositoryInfo.md) |  | [optional] 
**targetRepo** | [**RepositoryInfo**](RepositoryInfo.md) |  | [optional] 


<a name="StatusEnum"></a>
## Enum: StatusEnum


* `OPEN` (value: `"OPEN"`)

* `CLOSED` (value: `"CLOSED"`)

* `MERGED` (value: `"MERGED"`)




<a name="MergeableEnum"></a>
## Enum: MergeableEnum


* `MERGEABLE` (value: `"MERGEABLE"`)

* `UNMERGEABLE` (value: `"UNMERGEABLE"`)

* `UNKNOWN` (value: `"UNKNOWN"`)




