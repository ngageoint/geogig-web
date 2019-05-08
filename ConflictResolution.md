# Conflict resolution

Conflicts may happen when two branches are being merged together, and the same feature has been edited in both branches with conflicting changes. For instance:

* The feature has been removed in one from the branches, and edited in the other, then you must decide which one to keep, the deleted feature or the edited feature.
* The same attribute of the feature has been edited in both branches, with different values. Then you must decide which of the two values to keep, or assign a new one altogether.
 
## Conflicts on Pull Requests

Pull requests represent a call to incorporate the changes applied one branch (issuer) onto the receiving branch (target). A pull request can only be issued from a branch that has a common history with the target branch. That is, they're diverging branches but do have at least one point in common in their history topology. 

These two branches may belong to the same repository or not. More over, in the case of different repositories, they may be on the same "store" (i.e. database) or not.

The pull request life cycle ends when it's either merged or closed without merging (hence misregarded).

Merging a pull request means  applying the issuer branch changes on top of the receiving branch history, by means of a merge commit. And merging can only be done on a branch from a history snapshot (i.e. another branch, a commit identifier, a tag) that exists on the same repository. Hence, an intermediate step in merging a pull request is to "fetch" the missing history from the issuer branch onto the target repository.

By the other hand, checking whether the two branches will have conflicts if they were to be merged, can be done without any data transfer between repositories.

Checking for the "mergeable" status of a pull request is done by a `GET /repos/{user}/{repo}/pulls/{pr}/merge` call, while calling to actually merge the pull request by a `POST /repos/{user}/{repo}/pulls/{pr}/merge` call, where the `{user}`, `{repo}`, and `{pr}` parameters address the pull request on the target repository.

At any point in time, before merging the pull request, the two branches may become "unmergeable" due to conflicting changes.
For example, lets say a pull request was issued yesterday and had no conflicts with the target branch. But today the target branch received new edits that result in conflicting changes with the state of the pull request issuer branch.

The question of who's responsible to fix the conflicts so that the pull request can be back to a mergeable status depends on the workflow being followed. Most of the time the person issuing the pull request is in charge of fixing merge conflicts and re-submit the pull request, but it's also possible that the person in charge of accepting the pull request would want to fix it and merge it.

Either way, as previously mentioned, the missing data will need to be transferred to the repository where the conflicts are to be resolved. This is not a problem if the PR targets a branch on the same repository, or of the two repositories belong to the same "store", since they will share the revision objects database and hence no actual data transfer is needed. But it could be relevant when the PR targets a repository on a different store than the issuer branch owner.

All things considered, for the sake of usability, we'll adhere to the following workflow of resolving conflicts on the issuer repository. 
This addresses the more important concern of not bloating upstream repositories with too many dangling objects, given most of the pull requests will flow from less to more important repositories.
The concern of possible excessive data transfer from the upstream repository to the issuer repository will be addressed at a later stage by allowing and encouraging forks to be shallow and sparse whenever possible.

1.  When a PR is issued, a transaction is created on the issuer repo and the merge is attempted there, by first pulling from the target repository.
1. The transaction is left open on the issuer repository until the PR is merged and/or closed. Effectively fixing the PR to the state the issuer branch was at when submitted. The transaction identifier is added as part of the PR state.
1. If the merge succeeds, the PR is updated to contain a reference to the test-merge commit, and the state of the issuer and target branches it was created from.
1. If the merge fails, the transaction will contain the conflicts. This is because it is not allowed to have conflicts outside a transaction (given we need to support concurrent access and edits to repositories). This also allows to resume the transaction at any time and query and resolve the conflicts.
The conflicts or a PR are obtained by `GET /repos/{user}/{repo}/pulls/{pr}/conflicts`, and result in a list of `ConflictInfo` which their `ConflictDetail` present and containing the three `RevisionFeature`s (ancestor, ours, theirs) (ours or theirs could be null in case the feature was deleted at either end);
1. Each conflict is resolved in one of these two ways:
	1. By sending an array of `FeaturePatch` [1] objects using `PATCH	/repos/{user}/{repo}/pulls/{pr}/conflicts`
	1.  By setting the `ConflictInfo.ConflictDetails.ours` value of a returned `ConflictInfo` to the desired state and calling `PUT /repos/{user}/{repo}/pulls/{pr}/conflicts` with the array of `ConflictInfo` as the request body.
Either way, the server will stage the resolved feature state and remove the conflict.
1. Once the conflicts are resolved, it is time to re-attempt the merge commit. The system will launch the test-merge job immediately once the last conflict is resolved, and so will whenever new commits are made to the target branch, in order to keep the PR up to date.



[1]
```
  FeaturePatch:
    description: 'Associative array of attribute name to Value, representing a patch format for a RevisionFeature'
    type: object
    properties:
      path:
        description: 'Path to the feature identifier (e.g. roads/1)'
        type: string
    additionalProperties:
      $ref: '#/definitions/Value'
```













