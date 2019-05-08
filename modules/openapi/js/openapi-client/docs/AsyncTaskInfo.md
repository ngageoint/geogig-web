# Geogig.AsyncTaskInfo

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  | [optional] 
**status** | **String** |  | [optional] 
**description** | **String** |  | [optional] 
**progress** | [**ProgressInfo**](ProgressInfo.md) |  | [optional] 
**lastUpdated** | **Date** | When was the last time that progress on this task was reported | [optional] 
**scheduledAt** | **Date** | At what time was this task scheduled for execution | [optional] 
**startedAt** | **Date** | At what time was this task execution started | [optional] 
**finishedAt** | **Date** | At what time was this task finished. Null if not yet finished. | [optional] 
**repository** | [**RepositoryInfo**](RepositoryInfo.md) |  | [optional] 
**startedBy** | [**UserInfo**](UserInfo.md) |  | [optional] 
**abortedBy** | [**UserInfo**](UserInfo.md) |  | [optional] 
**transaction** | [**TransactionInfo**](TransactionInfo.md) |  | [optional] 
**result** | [**TaskResult**](TaskResult.md) |  | [optional] 
**error** | [**Error**](Error.md) |  | [optional] 


<a name="StatusEnum"></a>
## Enum: StatusEnum


* `SCHEDULED` (value: `"SCHEDULED"`)

* `RUNNING` (value: `"RUNNING"`)

* `COMPLETE` (value: `"COMPLETE"`)

* `FAILED` (value: `"FAILED"`)

* `ABORTING` (value: `"ABORTING"`)

* `ABORTED` (value: `"ABORTED"`)




