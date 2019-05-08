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
    root.Geogig.TaskResult = factory(root.Geogig.ApiClient);
  }
}(this, function(ApiClient) {
  'use strict';




  /**
   * The TaskResult model module.
   * @module model/TaskResult
   * @version 0.1-SNAPSHOT
   */

  /**
   * Constructs a new <code>TaskResult</code>.
   * A marker interface to domain objects that can be a result of an AsyncTask, given Swagger 2 does not support oneOf, which we would rather use on AsyncTaskInfo.result
   * @alias module:model/TaskResult
   * @class
   */
  var exports = function() {
    var _this = this;


  };

  /**
   * Constructs a <code>TaskResult</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/TaskResult} obj Optional instance to populate.
   * @return {module:model/TaskResult} The populated <code>TaskResult</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();

      if (data.hasOwnProperty('taskResultType')) {
        obj['taskResultType'] = ApiClient.convertToType(data['taskResultType'], 'String');
      }
    }
    return obj;
  }

  /**
   * @member {String} taskResultType
   */
  exports.prototype['taskResultType'] = undefined;



  return exports;
}));


