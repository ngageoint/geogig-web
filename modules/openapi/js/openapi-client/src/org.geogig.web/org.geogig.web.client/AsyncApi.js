/**
 * GeoGig Web API
 * GeoGig Web API.  You can find out more about GeoGig at [http://geogig.org](http://geogig.org)
 *
 * OpenAPI spec version: 0.1.0
 * Contact: groldan@boundlessgeo.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 *
 * Swagger Codegen version: 2.3.1
 *
 * Do not edit the class manually.
 *
 */

(function(root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['org.geogig.web/ApiClient', 'org.geogig.web/org.geogig.web.model/AsyncTaskInfo', 'org.geogig.web/org.geogig.web.model/ProgressInfo'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('../org.geogig.web.model/AsyncTaskInfo'), require('../org.geogig.web.model/ProgressInfo'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.AsyncApi = factory(root.Geogig.ApiClient, root.Geogig.AsyncTaskInfo, root.Geogig.ProgressInfo);
  }
}(this, function(ApiClient, AsyncTaskInfo, ProgressInfo) {
  'use strict';

  /**
   * Async service.
   * @module org.geogig.web/org.geogig.web.client/AsyncApi
   * @version 1.0-SNAPSHOT
   */

  /**
   * Constructs a new AsyncApi. 
   * @alias module:org.geogig.web/org.geogig.web.client/AsyncApi
   * @class
   * @param {module:org.geogig.web/ApiClient} [apiClient] Optional API client implementation to use,
   * default to {@link module:org.geogig.web/ApiClient#instance} if unspecified.
   */
  var exports = function(apiClient) {
    this.apiClient = apiClient || ApiClient.instance;


    /**
     * Callback function to receive the result of the abortTask operation.
     * @callback module:org.geogig.web/org.geogig.web.client/AsyncApi~abortTaskCallback
     * @param {String} error Error message, if any.
     * @param {module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo} data The data returned by the service call.
     * @param {String} response The complete HTTP response.
     */

    /**
     * Abort a running or scheduled task
     * Abort a running or scheduled task. If the task is already finished, the operation has no effect and its current state is returned. If the tasks is not finished, the returned task info status might be either ABORTING or ABORTED
     * @param {String} taskId Async task identifier
     * @param {module:org.geogig.web/org.geogig.web.client/AsyncApi~abortTaskCallback} callback The callback function, accepting three arguments: error, data, response
     * data is of type: {@link module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo}
     */
    this.abortTask = function(taskId, callback) {
      var postBody = null;

      // verify the required parameter 'taskId' is set
      if (taskId === undefined || taskId === null) {
        throw new Error("Missing the required parameter 'taskId' when calling abortTask");
      }


      var pathParams = {
        'taskId': taskId
      };
      var queryParams = {
      };
      var collectionQueryParams = {
      };
      var headerParams = {
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = AsyncTaskInfo;

      return this.apiClient.callApi(
        '/tasks/{taskId}/abort', 'POST',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType, callback
      );
    }

    /**
     * Callback function to receive the result of the commitTransaction operation.
     * @callback module:org.geogig.web/org.geogig.web.client/AsyncApi~commitTransactionCallback
     * @param {String} error Error message, if any.
     * @param {module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo} data The data returned by the service call.
     * @param {String} response The complete HTTP response.
     */

    /**
     * Commit transaction. Async operation.
     * When committing a transaction
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {String} transactionId transaction identifier
     * @param {Object} opts Optional parameters
     * @param {String} opts.messageTitle A short (no more than 100 characters) title to summarize the reason this commit is being made
     * @param {String} opts.messageDescription A possibly larger, even spanning multiple paragraphs, description of the reason this commit is applied.
     * @param {module:org.geogig.web/org.geogig.web.client/AsyncApi~commitTransactionCallback} callback The callback function, accepting three arguments: error, data, response
     * data is of type: {@link module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo}
     */
    this.commitTransaction = function(user, repo, transactionId, opts, callback) {
      opts = opts || {};
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling commitTransaction");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling commitTransaction");
      }

      // verify the required parameter 'transactionId' is set
      if (transactionId === undefined || transactionId === null) {
        throw new Error("Missing the required parameter 'transactionId' when calling commitTransaction");
      }


      var pathParams = {
        'user': user,
        'repo': repo,
        'transactionId': transactionId
      };
      var queryParams = {
      };
      var collectionQueryParams = {
      };
      var headerParams = {
        'messageTitle': opts['messageTitle'],
        'messageDescription': opts['messageDescription']
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = AsyncTaskInfo;

      return this.apiClient.callApi(
        '/transactions/{user}/{repo}/{transactionId}/commit', 'POST',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType, callback
      );
    }

    /**
     * Callback function to receive the result of the forkRepository operation.
     * @callback module:org.geogig.web/org.geogig.web.client/AsyncApi~forkRepositoryCallback
     * @param {String} error Error message, if any.
     * @param {module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo} data The data returned by the service call.
     * @param {String} response The complete HTTP response.
     */

    /**
     * Fork this repository. Async operation.
     * Forks this repository asynchronously to the authenticated user&#39;s account. Forking is the same than cloning but the term is used in this context to differentiate cloning to any allowed remote URI vs creating clone of this repository inside this server.
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {Object} opts Optional parameters
     * @param {String} opts.forkName Optional name for the forked repository under the authenticated user&#39;s account. If not provided defaults to the source repository name
     * @param {String} opts.targetStore Name of the repository store writable to the authenticated user to fork this repo to. If not provided, the current user&#39;s default store is used.
     * @param {module:org.geogig.web/org.geogig.web.client/AsyncApi~forkRepositoryCallback} callback The callback function, accepting three arguments: error, data, response
     * data is of type: {@link module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo}
     */
    this.forkRepository = function(user, repo, opts, callback) {
      opts = opts || {};
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling forkRepository");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling forkRepository");
      }


      var pathParams = {
        'user': user,
        'repo': repo
      };
      var queryParams = {
        'forkName': opts['forkName'],
        'targetStore': opts['targetStore'],
      };
      var collectionQueryParams = {
      };
      var headerParams = {
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json'];
      var returnType = AsyncTaskInfo;

      return this.apiClient.callApi(
        '/repos/{user}/{repo}/forks', 'POST',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType, callback
      );
    }

    /**
     * Callback function to receive the result of the getTaskInfo operation.
     * @callback module:org.geogig.web/org.geogig.web.client/AsyncApi~getTaskInfoCallback
     * @param {String} error Error message, if any.
     * @param {module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo} data The data returned by the service call.
     * @param {String} response The complete HTTP response.
     */

    /**
     * Access status info for a given task
     * @param {String} taskId Async task identifier
     * @param {Object} opts Optional parameters
     * @param {Boolean} opts.prune If provided and true, the task information is pruned (deleted) if the operation is complete (either successfully or not)
     * @param {module:org.geogig.web/org.geogig.web.client/AsyncApi~getTaskInfoCallback} callback The callback function, accepting three arguments: error, data, response
     * data is of type: {@link module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo}
     */
    this.getTaskInfo = function(taskId, opts, callback) {
      opts = opts || {};
      var postBody = null;

      // verify the required parameter 'taskId' is set
      if (taskId === undefined || taskId === null) {
        throw new Error("Missing the required parameter 'taskId' when calling getTaskInfo");
      }


      var pathParams = {
        'taskId': taskId
      };
      var queryParams = {
        'prune': opts['prune'],
      };
      var collectionQueryParams = {
      };
      var headerParams = {
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = AsyncTaskInfo;

      return this.apiClient.callApi(
        '/tasks/{taskId}', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType, callback
      );
    }

    /**
     * Callback function to receive the result of the getTaskProgress operation.
     * @callback module:org.geogig.web/org.geogig.web.client/AsyncApi~getTaskProgressCallback
     * @param {String} error Error message, if any.
     * @param {module:org.geogig.web/org.geogig.web.model/ProgressInfo} data The data returned by the service call.
     * @param {String} response The complete HTTP response.
     */

    /**
     * Get task progress info
     * @param {String} taskId Async task identifier
     * @param {module:org.geogig.web/org.geogig.web.client/AsyncApi~getTaskProgressCallback} callback The callback function, accepting three arguments: error, data, response
     * data is of type: {@link module:org.geogig.web/org.geogig.web.model/ProgressInfo}
     */
    this.getTaskProgress = function(taskId, callback) {
      var postBody = null;

      // verify the required parameter 'taskId' is set
      if (taskId === undefined || taskId === null) {
        throw new Error("Missing the required parameter 'taskId' when calling getTaskProgress");
      }


      var pathParams = {
        'taskId': taskId
      };
      var queryParams = {
      };
      var collectionQueryParams = {
      };
      var headerParams = {
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = ProgressInfo;

      return this.apiClient.callApi(
        '/tasks/{taskId}/progress', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType, callback
      );
    }

    /**
     * Callback function to receive the result of the listTasks operation.
     * @callback module:org.geogig.web/org.geogig.web.client/AsyncApi~listTasksCallback
     * @param {String} error Error message, if any.
     * @param {Array.<module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo>} data The data returned by the service call.
     * @param {String} response The complete HTTP response.
     */

    /**
     * List summary information for current asynchronous tasks
     * @param {module:org.geogig.web/org.geogig.web.client/AsyncApi~listTasksCallback} callback The callback function, accepting three arguments: error, data, response
     * data is of type: {@link Array.<module:org.geogig.web/org.geogig.web.model/AsyncTaskInfo>}
     */
    this.listTasks = function(callback) {
      var postBody = null;


      var pathParams = {
      };
      var queryParams = {
      };
      var collectionQueryParams = {
      };
      var headerParams = {
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = [AsyncTaskInfo];

      return this.apiClient.callApi(
        '/tasks', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType, callback
      );
    }

    /**
     * Callback function to receive the result of the pruneTask operation.
     * @callback module:org.geogig.web/org.geogig.web.client/AsyncApi~pruneTaskCallback
     * @param {String} error Error message, if any.
     * @param data This operation does not return a value.
     * @param {String} response The complete HTTP response.
     */

    /**
     * Prune a task if finished
     * If the task is finished, then deletes its information, fail otherwise
     * @param {String} taskId Async task identifier
     * @param {module:org.geogig.web/org.geogig.web.client/AsyncApi~pruneTaskCallback} callback The callback function, accepting three arguments: error, data, response
     */
    this.pruneTask = function(taskId, callback) {
      var postBody = null;

      // verify the required parameter 'taskId' is set
      if (taskId === undefined || taskId === null) {
        throw new Error("Missing the required parameter 'taskId' when calling pruneTask");
      }


      var pathParams = {
        'taskId': taskId
      };
      var queryParams = {
      };
      var collectionQueryParams = {
      };
      var headerParams = {
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = null;

      return this.apiClient.callApi(
        '/tasks/{taskId}', 'DELETE',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType, callback
      );
    }
  };

  return exports;
}));