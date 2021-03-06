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
    define(['ApiClient', 'model/Error', 'model/ProgressInfo', 'model/RepositoryInfo', 'model/TaskResult', 'model/TransactionInfo', 'model/UserInfo'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('./Error'), require('./ProgressInfo'), require('./RepositoryInfo'), require('./TaskResult'), require('./TransactionInfo'), require('./UserInfo'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.AsyncTaskInfo = factory(root.Geogig.ApiClient, root.Geogig.Error, root.Geogig.ProgressInfo, root.Geogig.RepositoryInfo, root.Geogig.TaskResult, root.Geogig.TransactionInfo, root.Geogig.UserInfo);
  }
}(this, function(ApiClient, Error, ProgressInfo, RepositoryInfo, TaskResult, TransactionInfo, UserInfo) {
  'use strict';




  /**
   * The AsyncTaskInfo model module.
   * @module model/AsyncTaskInfo
   * @version 0.1-SNAPSHOT
   */

  /**
   * Constructs a new <code>AsyncTaskInfo</code>.
   * @alias module:model/AsyncTaskInfo
   * @class
   */
  var exports = function() {
    var _this = this;















  };

  /**
   * Constructs a <code>AsyncTaskInfo</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/AsyncTaskInfo} obj Optional instance to populate.
   * @return {module:model/AsyncTaskInfo} The populated <code>AsyncTaskInfo</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();

      if (data.hasOwnProperty('id')) {
        obj['id'] = ApiClient.convertToType(data['id'], 'String');
      }
      if (data.hasOwnProperty('status')) {
        obj['status'] = ApiClient.convertToType(data['status'], 'String');
      }
      if (data.hasOwnProperty('description')) {
        obj['description'] = ApiClient.convertToType(data['description'], 'String');
      }
      if (data.hasOwnProperty('progress')) {
        obj['progress'] = ProgressInfo.constructFromObject(data['progress']);
      }
      if (data.hasOwnProperty('last_updated')) {
        obj['last_updated'] = ApiClient.convertToType(data['last_updated'], 'Date');
      }
      if (data.hasOwnProperty('scheduled_at')) {
        obj['scheduled_at'] = ApiClient.convertToType(data['scheduled_at'], 'Date');
      }
      if (data.hasOwnProperty('started_at')) {
        obj['started_at'] = ApiClient.convertToType(data['started_at'], 'Date');
      }
      if (data.hasOwnProperty('finished_at')) {
        obj['finished_at'] = ApiClient.convertToType(data['finished_at'], 'Date');
      }
      if (data.hasOwnProperty('repository')) {
        obj['repository'] = RepositoryInfo.constructFromObject(data['repository']);
      }
      if (data.hasOwnProperty('started_by')) {
        obj['started_by'] = UserInfo.constructFromObject(data['started_by']);
      }
      if (data.hasOwnProperty('aborted_by')) {
        obj['aborted_by'] = UserInfo.constructFromObject(data['aborted_by']);
      }
      if (data.hasOwnProperty('transaction')) {
        obj['transaction'] = TransactionInfo.constructFromObject(data['transaction']);
      }
      if (data.hasOwnProperty('result')) {
        obj['result'] = TaskResult.constructFromObject(data['result']);
      }
      if (data.hasOwnProperty('error')) {
        obj['error'] = Error.constructFromObject(data['error']);
      }
    }
    return obj;
  }

  /**
   * @member {String} id
   */
  exports.prototype['id'] = undefined;
  /**
   * @member {module:model/AsyncTaskInfo.StatusEnum} status
   */
  exports.prototype['status'] = undefined;
  /**
   * @member {String} description
   */
  exports.prototype['description'] = undefined;
  /**
   * @member {module:model/ProgressInfo} progress
   */
  exports.prototype['progress'] = undefined;
  /**
   * When was the last time that progress on this task was reported
   * @member {Date} last_updated
   */
  exports.prototype['last_updated'] = undefined;
  /**
   * At what time was this task scheduled for execution
   * @member {Date} scheduled_at
   */
  exports.prototype['scheduled_at'] = undefined;
  /**
   * At what time was this task execution started
   * @member {Date} started_at
   */
  exports.prototype['started_at'] = undefined;
  /**
   * At what time was this task finished. Null if not yet finished.
   * @member {Date} finished_at
   */
  exports.prototype['finished_at'] = undefined;
  /**
   * @member {module:model/RepositoryInfo} repository
   */
  exports.prototype['repository'] = undefined;
  /**
   * @member {module:model/UserInfo} started_by
   */
  exports.prototype['started_by'] = undefined;
  /**
   * @member {module:model/UserInfo} aborted_by
   */
  exports.prototype['aborted_by'] = undefined;
  /**
   * @member {module:model/TransactionInfo} transaction
   */
  exports.prototype['transaction'] = undefined;
  /**
   * @member {module:model/TaskResult} result
   */
  exports.prototype['result'] = undefined;
  /**
   * @member {module:model/Error} error
   */
  exports.prototype['error'] = undefined;


  /**
   * Allowed values for the <code>status</code> property.
   * @enum {String}
   * @readonly
   */
  exports.StatusEnum = {
    /**
     * value: "SCHEDULED"
     * @const
     */
    "SCHEDULED": "SCHEDULED",
    /**
     * value: "RUNNING"
     * @const
     */
    "RUNNING": "RUNNING",
    /**
     * value: "COMPLETE"
     * @const
     */
    "COMPLETE": "COMPLETE",
    /**
     * value: "FAILED"
     * @const
     */
    "FAILED": "FAILED",
    /**
     * value: "ABORTING"
     * @const
     */
    "ABORTING": "ABORTING",
    /**
     * value: "ABORTED"
     * @const
     */
    "ABORTED": "ABORTED"  };


  return exports;
}));


