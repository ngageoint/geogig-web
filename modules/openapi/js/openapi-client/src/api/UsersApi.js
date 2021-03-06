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
    define(['ApiClient', 'model/RepositoryInfo', 'model/UserInfo'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('../model/RepositoryInfo'), require('../model/UserInfo'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.UsersApi = factory(root.Geogig.ApiClient, root.Geogig.RepositoryInfo, root.Geogig.UserInfo);
  }
}(this, function(ApiClient, RepositoryInfo, UserInfo) {
  'use strict';

  /**
   * Users service.
   * @module api/UsersApi
   * @version 0.1-SNAPSHOT
   */

  /**
   * Constructs a new UsersApi. 
   * @alias module:api/UsersApi
   * @class
   * @param {module:ApiClient} [apiClient] Optional API client implementation to use,
   * default to {@link module:ApiClient#instance} if unspecified.
   */
  var exports = function(apiClient) {
    this.apiClient = apiClient || ApiClient.instance;



    /**
     * Creates a new user
     * @param {module:model/UserInfo} userInfo The repository to create
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/UserInfo} and HTTP response
     */
    this.createUserWithHttpInfo = function(userInfo) {
      var postBody = userInfo;

      // verify the required parameter 'userInfo' is set
      if (userInfo === undefined || userInfo === null) {
        throw new Error("Missing the required parameter 'userInfo' when calling createUser");
      }


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
      var contentTypes = ['application/json'];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = UserInfo;

      return this.apiClient.callApi(
        '/users', 'POST',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Creates a new user
     * @param {module:model/UserInfo} userInfo The repository to create
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/UserInfo}
     */
    this.createUser = function(userInfo) {
      return this.createUserWithHttpInfo(userInfo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Delete user
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing HTTP response
     */
    this.deleteUserWithHttpInfo = function(user) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling deleteUser");
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
      var returnType = null;

      return this.apiClient.callApi(
        '/users/{user}', 'DELETE',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Delete user
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}
     */
    this.deleteUser = function(user) {
      return this.deleteUserWithHttpInfo(user)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Follow a user
     * @param {String} user repository login owner
     * @param {String} followee The name of the user to follow
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link 'Boolean'} and HTTP response
     */
    this.followWithHttpInfo = function(user, followee) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling follow");
      }

      // verify the required parameter 'followee' is set
      if (followee === undefined || followee === null) {
        throw new Error("Missing the required parameter 'followee' when calling follow");
      }


      var pathParams = {
        'user': user,
        'followee': followee
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
      var returnType = 'Boolean';

      return this.apiClient.callApi(
        '/users/{user}/following/{followee}', 'PUT',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Follow a user
     * @param {String} user repository login owner
     * @param {String} followee The name of the user to follow
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link 'Boolean'}
     */
    this.follow = function(user, followee) {
      return this.followWithHttpInfo(user, followee)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Test whether :user is a follower of :followee
     * @param {String} user repository login owner
     * @param {String} followee The name of the user to follow
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link 'Boolean'} and HTTP response
     */
    this.followsWithHttpInfo = function(user, followee) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling follows");
      }

      // verify the required parameter 'followee' is set
      if (followee === undefined || followee === null) {
        throw new Error("Missing the required parameter 'followee' when calling follows");
      }


      var pathParams = {
        'user': user,
        'followee': followee
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
      var returnType = 'Boolean';

      return this.apiClient.callApi(
        '/users/{user}/following/{followee}', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Test whether :user is a follower of :followee
     * @param {String} user repository login owner
     * @param {String} followee The name of the user to follow
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link 'Boolean'}
     */
    this.follows = function(user, followee) {
      return this.followsWithHttpInfo(user, followee)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Get the request&#39;s authenticated user info
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/UserInfo} and HTTP response
     */
    this.getSelfWithHttpInfo = function() {
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
      var returnType = UserInfo;

      return this.apiClient.callApi(
        '/user', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Get the request&#39;s authenticated user info
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/UserInfo}
     */
    this.getSelf = function() {
      return this.getSelfWithHttpInfo()
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Obtain either public or public + private user information depending on auth credentials
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/UserInfo} and HTTP response
     */
    this.getUserWithHttpInfo = function(user) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling getUser");
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
      var returnType = UserInfo;

      return this.apiClient.callApi(
        '/users/{user}', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Obtain either public or public + private user information depending on auth credentials
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/UserInfo}
     */
    this.getUser = function(user) {
      return this.getUserWithHttpInfo(user)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List the repositories owned by the given user
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link Array.<module:model/RepositoryInfo>} and HTTP response
     */
    this.getUserRepositoriesWithHttpInfo = function(user) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling getUserRepositories");
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
        '/users/{user}/repos', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List the repositories owned by the given user
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link Array.<module:model/RepositoryInfo>}
     */
    this.getUserRepositories = function(user) {
      return this.getUserRepositoriesWithHttpInfo(user)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List summary information for available collections of repositories. Only stores visible to the current user are listed.
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link Array.<module:model/UserInfo>} and HTTP response
     */
    this.getUsersWithHttpInfo = function() {
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
      var returnType = [UserInfo];

      return this.apiClient.callApi(
        '/users', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List summary information for available collections of repositories. Only stores visible to the current user are listed.
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link Array.<module:model/UserInfo>}
     */
    this.getUsers = function() {
      return this.getUsersWithHttpInfo()
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List users following the given user
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link Array.<module:model/UserInfo>} and HTTP response
     */
    this.listFollowersWithHttpInfo = function(user) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling listFollowers");
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
      var returnType = [UserInfo];

      return this.apiClient.callApi(
        '/users/{user}/followers', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List users following the given user
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link Array.<module:model/UserInfo>}
     */
    this.listFollowers = function(user) {
      return this.listFollowersWithHttpInfo(user)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * List of users the given user follows
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link Array.<module:model/UserInfo>} and HTTP response
     */
    this.listFollowingWithHttpInfo = function(user) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling listFollowing");
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
      var returnType = [UserInfo];

      return this.apiClient.callApi(
        '/users/{user}/following', 'GET',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * List of users the given user follows
     * @param {String} user repository login owner
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link Array.<module:model/UserInfo>}
     */
    this.listFollowing = function(user) {
      return this.listFollowingWithHttpInfo(user)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Updates a user info
     * @param {module:model/UserInfo} userInfo The updated user information
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link module:model/UserInfo} and HTTP response
     */
    this.modifyUserWithHttpInfo = function(userInfo) {
      var postBody = userInfo;

      // verify the required parameter 'userInfo' is set
      if (userInfo === undefined || userInfo === null) {
        throw new Error("Missing the required parameter 'userInfo' when calling modifyUser");
      }


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
      var contentTypes = ['application/json'];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = UserInfo;

      return this.apiClient.callApi(
        '/users', 'PUT',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Updates a user info
     * @param {module:model/UserInfo} userInfo The updated user information
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link module:model/UserInfo}
     */
    this.modifyUser = function(userInfo) {
      return this.modifyUserWithHttpInfo(userInfo)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Resets the HTTP Basic password for the user. The request issuer must be either a site admin or the user itself. Note this is a temporary meassure until more advanced auth services are used
     * @param {String} user repository login owner
     * @param {Object} opts Optional parameters
     * @param {String} opts.newPassword 
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing HTTP response
     */
    this.resetPasswordWithHttpInfo = function(user, opts) {
      opts = opts || {};
      var postBody = opts['newPassword'];

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling resetPassword");
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
      var contentTypes = ['application/json'];
      var accepts = ['application/json', 'application/x-jackson-smile'];
      var returnType = null;

      return this.apiClient.callApi(
        '/users/{user}/password', 'PUT',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Resets the HTTP Basic password for the user. The request issuer must be either a site admin or the user itself. Note this is a temporary meassure until more advanced auth services are used
     * @param {String} user repository login owner
     * @param {Object} opts Optional parameters
     * @param {String} opts.newPassword 
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}
     */
    this.resetPassword = function(user, opts) {
      return this.resetPasswordWithHttpInfo(user, opts)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }


    /**
     * Unfollow a user
     * @param {String} user repository login owner
     * @param {String} followee The name of the user to follow
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with an object containing data of type {@link 'Boolean'} and HTTP response
     */
    this.unfollowWithHttpInfo = function(user, followee) {
      var postBody = null;

      // verify the required parameter 'user' is set
      if (user === undefined || user === null) {
        throw new Error("Missing the required parameter 'user' when calling unfollow");
      }

      // verify the required parameter 'followee' is set
      if (followee === undefined || followee === null) {
        throw new Error("Missing the required parameter 'followee' when calling unfollow");
      }


      var pathParams = {
        'user': user,
        'followee': followee
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
      var returnType = 'Boolean';

      return this.apiClient.callApi(
        '/users/{user}/following/{followee}', 'DELETE',
        pathParams, queryParams, collectionQueryParams, headerParams, formParams, postBody,
        authNames, contentTypes, accepts, returnType
      );
    }

    /**
     * Unfollow a user
     * @param {String} user repository login owner
     * @param {String} followee The name of the user to follow
     * @return {Promise} a {@link https://www.promisejs.org/|Promise}, with data of type {@link 'Boolean'}
     */
    this.unfollow = function(user, followee) {
      return this.unfollowWithHttpInfo(user, followee)
        .then(function(response_and_data) {
          return response_and_data.data;
        });
    }
  };

  return exports;
}));
