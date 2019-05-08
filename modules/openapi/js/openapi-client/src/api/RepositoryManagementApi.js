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
 * Swagger Codegen version: unset
 *
 * Do not edit the class manually.
 *
 */

(function(root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['ApiClient', 'model/AsyncTaskInfo', 'model/RepositoryInfo', 'model/UserInfo'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('../model/AsyncTaskInfo'), require('../model/RepositoryInfo'), require('../model/UserInfo'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.RepositoryManagementApi = factory(root.Geogig.ApiClient, root.Geogig.AsyncTaskInfo, root.Geogig.RepositoryInfo, root.Geogig.UserInfo);
  }
}(this, function(ApiClient, AsyncTaskInfo, RepositoryInfo, UserInfo) {
  'use strict';

  /**
   * RepositoryManagement service.
   * @module api/RepositoryManagementApi
   * @version 0.1-SNAPSHOT
   */

  /**
   * Constructs a new RepositoryManagementApi. 
   * @alias module:api/RepositoryManagementApi
   * @class
   * @param {module:ApiClient} [apiClient] Optional API client implementation to use,
   * default to {@link module:ApiClient#instance} if unspecified.
   */
  var exports = function(apiClient) {
    this.apiClient = apiClient || ApiClient.instance;



    /**
     * Number of users watching this repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link 'Number'} and HTTP response
     */
    this.countWatchersWithHttpInfo = function(user, repo) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling countWatchers");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling countWatchers");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
      var returnType = 'Number';

      return this.apiClient.callApi(
        '/repos/{user}/{repo}/watchers/count', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Number of users watching this repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link 'Number'}
     */
    this.countWatchers = function(user, repo) {
      return this.countWatchersWithHttpInfo(user, repo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Create a new repository
     * Create a new repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {Object} opts Optional parameters
     * @param {String} opts.targetStore Optionally, the name of the target store where to save the repo. Defaults to the owner&#39;s default store.
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/RepositoryInfo} and HTTP response
     */
    this.createRepositoryWithHttpInfo = function(user, repo, opts) {
      opts = opts || {};
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling createRepository");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling createRepository");
      }


      var pathParams = {
        'user': user,
        'repo': repo
      };
      var queryParams = {
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
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = RepositoryInfo;

      return this.apiClient.callApi(
        '/repos/{user}/{repo}', 'POST',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Create a new repository
     * Create a new repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {Object} opts Optional parameters
     * @param {String} opts.targetStore Optionally, the name of the target store where to save the repo. Defaults to the owner&#39;s default store.
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/RepositoryInfo}
     */
    this.createRepository = function(user, repo, opts) {
      return this.createRepositoryWithHttpInfo(user, repo, opts)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Delete repository
     * Deletes the repository addressed by the request path
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing HTTP response
     */
    this.deleteRepositoryWithHttpInfo = function(user, repo) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling deleteRepository");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling deleteRepository");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
        '/repos/{user}/{repo}', 'DELETE',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Delete repository
     * Deletes the repository addressed by the request path
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}
     */
    this.deleteRepository = function(user, repo) {
      return this.deleteRepositoryWithHttpInfo(user, repo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Fork this repository. Async operation.
     * Forks this repository asynchronously to the authenticated user&#39;s account. Forking is the same than cloning but the term is used in this context to differentiate cloning to any allowed remote URI vs creating clone of this repository inside this server.
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {Object} opts Optional parameters
     * @param {String} opts.forkName Optional name for the forked repository under the authenticated user&#39;s account. If not provided defaults to the source repository name
     * @param {String} opts.targetStore Name of the repository store writable to the authenticated user to fork this repo to. If not provided, the current user&#39;s default store is used.
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/AsyncTaskInfo} and HTTP response
     */
    this.forkRepositoryWithHttpInfo = function(user, repo, opts) {
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
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Fork this repository. Async operation.
     * Forks this repository asynchronously to the authenticated user&#39;s account. Forking is the same than cloning but the term is used in this context to differentiate cloning to any allowed remote URI vs creating clone of this repository inside this server.
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {Object} opts Optional parameters
     * @param {String} opts.forkName Optional name for the forked repository under the authenticated user&#39;s account. If not provided defaults to the source repository name
     * @param {String} opts.targetStore Name of the repository store writable to the authenticated user to fork this repo to. If not provided, the current user&#39;s default store is used.
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/AsyncTaskInfo}
     */
    this.forkRepository = function(user, repo, opts) {
      return this.forkRepositoryWithHttpInfo(user, repo, opts)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Get full repository information
     * Obtain a manifest about the current status of the repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/RepositoryInfo} and HTTP response
     */
    this.getRepositoryWithHttpInfo = function(user, repo) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling getRepository");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling getRepository");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
      var returnType = RepositoryInfo;

      return this.apiClient.callApi(
        '/repos/{user}/{repo}', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Get full repository information
     * Obtain a manifest about the current status of the repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/RepositoryInfo}
     */
    this.getRepository = function(user, repo) {
      return this.getRepositoryWithHttpInfo(user, repo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List repositories that are forks of this one
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/RepositoryInfo} and HTTP response
     */
    this.listForksWithHttpInfo = function(user, repo) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling listForks");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling listForks");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
      var returnType = RepositoryInfo;

      return this.apiClient.callApi(
        '/repos/{user}/{repo}/forks', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List repositories that are forks of this one
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/RepositoryInfo}
     */
    this.listForks = function(user, repo) {
      return this.listForksWithHttpInfo(user, repo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List repositories summary information
     * @param {Object} opts Optional parameters
     * @param {Array.<String>} opts.topics 
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link Array.<module:model/RepositoryInfo>} and HTTP response
     */
    this.listRepositoriesWithHttpInfo = function(opts) {
      opts = opts || {};
      var postBody = null;


      var pathParams = {
      };
      var queryParams = {
      };
      var collectionQueryParams = {
        'topics': {
          value: opts['topics'],
          collectionFormat: 'csv'
        },
      };
      var headerParams = {
      };
      var formParams = {
      };

      var authNames = ['ApiKeyAuth', 'BasicAuth', 'OAuth2'];
      var contentTypes = [];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = [RepositoryInfo];

      return this.apiClient.callApi(
        '/repos', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List repositories summary information
     * @param {Object} opts Optional parameters
     * @param {Array.<String>} opts.topics 
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link Array.<module:model/RepositoryInfo>}
     */
    this.listRepositories = function(opts) {
      return this.listRepositoriesWithHttpInfo(opts)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List of available repositories summary information
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link Array.<module:model/RepositoryInfo>} and HTTP response
     */
    this.listUserRepositoriesWithHttpInfo = function(user) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling listUserRepositories");
      }


      var pathParams = {
        'user': user
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
      var returnType = [RepositoryInfo];

      return this.apiClient.callApi(
        '/repos/{user}', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List of available repositories summary information
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link Array.<module:model/RepositoryInfo>}
     */
    this.listUserRepositories = function(user) {
      return this.listUserRepositoriesWithHttpInfo(user)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List users watching this repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/UserInfo} and HTTP response
     */
    this.listWatchersWithHttpInfo = function(user, repo) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling listWatchers");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling listWatchers");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
      var returnType = UserInfo;

      return this.apiClient.callApi(
        '/repos/{user}/{repo}/watchers', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List users watching this repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/UserInfo}
     */
    this.listWatchers = function(user, repo) {
      return this.listWatchersWithHttpInfo(user, repo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Modify repository
     * Modify repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {module:model/RepositoryInfo} repository The new repository settings
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/RepositoryInfo} and HTTP response
     */
    this.modifyRepositoryWithHttpInfo = function(user, repo, repository) {
      var postBody = repository;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling modifyRepository");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling modifyRepository");
      }

      // verify the required parameter 'repository' is set
      if (repository === undefined || repository === null) {
        throw new Error("Missing the required parameter 'repository' when calling modifyRepository");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
      var contentTypes = ['application/json'];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = RepositoryInfo;

      return this.apiClient.callApi(
        '/repos/{user}/{repo}', 'PUT',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Modify repository
     * Modify repository
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @param {module:model/RepositoryInfo} repository The new repository settings
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/RepositoryInfo}
     */
    this.modifyRepository = function(user, repo, repository) {
      return this.modifyRepositoryWithHttpInfo(user, repo, repository)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Stop watching this repo.
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing HTTP response
     */
    this.unwatchRepositoryWithHttpInfo = function(user, repo) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling unwatchRepository");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling unwatchRepository");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
        '/repos/{user}/{repo}/watchers', 'DELETE',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Stop watching this repo.
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}
     */
    this.unwatchRepository = function(user, repo) {
      return this.unwatchRepositoryWithHttpInfo(user, repo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Start watching this repo.
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing HTTP response
     */
    this.watchRepositoryWithHttpInfo = function(user, repo) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling watchRepository");
      }

      // verify the required parameter 'repo' is set
      if (repo === undefined || repo === null) {
        throw new Error("Missing the required parameter 'repo' when calling watchRepository");
      }


      var pathParams = {
        'user': user,
        'repo': repo
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
        '/repos/{user}/{repo}/watchers', 'POST',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Start watching this repo.
     * @param {String} user repository login owner
     * @param {String} repo repository name
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}
     */
    this.watchRepository = function(user, repo) {
      return this.watchRepositoryWithHttpInfo(user, repo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }
  };

  return exports;
}));
