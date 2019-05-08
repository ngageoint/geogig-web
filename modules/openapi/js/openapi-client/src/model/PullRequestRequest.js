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
    define(['ApiClient'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.PullRequestRequest = factory(root.Geogig.ApiClient);
  }
}(this, function(ApiClient) {
  'use strict';




  /**
   * The PullRequestRequest model module.
   * @module model/PullRequestRequest
   * @version 0.1-SNAPSHOT
   */

  /**
   * Constructs a new <code>PullRequestRequest</code>.
   * @alias module:model/PullRequestRequest
   * @class
   * @param title {String} 
   */
  var exports = function(title) {
    var _this = this;

    _this['title'] = title;





  };

  /**
   * Constructs a <code>PullRequestRequest</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/PullRequestRequest} obj Optional instance to populate.
   * @return {module:model/PullRequestRequest} The populated <code>PullRequestRequest</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();

      if (data.hasOwnProperty('title')) {
        obj['title'] = ApiClient.convertToType(data['title'], 'String');
      }
      if (data.hasOwnProperty('description')) {
        obj['description'] = ApiClient.convertToType(data['description'], 'String');
      }
      if (data.hasOwnProperty('targetBranch')) {
        obj['targetBranch'] = ApiClient.convertToType(data['targetBranch'], 'String');
      }
      if (data.hasOwnProperty('sourceRepositoryOwner')) {
        obj['sourceRepositoryOwner'] = ApiClient.convertToType(data['sourceRepositoryOwner'], 'String');
      }
      if (data.hasOwnProperty('sourceRepositryName')) {
        obj['sourceRepositryName'] = ApiClient.convertToType(data['sourceRepositryName'], 'String');
      }
      if (data.hasOwnProperty('sourceRepositoryBranch')) {
        obj['sourceRepositoryBranch'] = ApiClient.convertToType(data['sourceRepositoryBranch'], 'String');
      }
    }
    return obj;
  }

  /**
   * @member {String} title
   */
  exports.prototype['title'] = undefined;
  /**
   * @member {String} description
   */
  exports.prototype['description'] = undefined;
  /**
   * @member {String} targetBranch
   */
  exports.prototype['targetBranch'] = undefined;
  /**
   * @member {String} sourceRepositoryOwner
   */
  exports.prototype['sourceRepositoryOwner'] = undefined;
  /**
   * @member {String} sourceRepositryName
   */
  exports.prototype['sourceRepositryName'] = undefined;
  /**
   * @member {String} sourceRepositoryBranch
   */
  exports.prototype['sourceRepositoryBranch'] = undefined;



  return exports;
}));

