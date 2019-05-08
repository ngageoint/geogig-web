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
    define(['org.geogig.web/ApiClient', 'org.geogig.web/org.geogig.web.model/StoreConnectionInfo'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('./StoreConnectionInfo'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.FileStoreInfo = factory(root.Geogig.ApiClient, root.Geogig.StoreConnectionInfo);
  }
}(this, function(ApiClient, StoreConnectionInfo) {
  'use strict';




  /**
   * The FileStoreInfo model module.
   * @module org.geogig.web/org.geogig.web.model/FileStoreInfo
   * @version 1.0-SNAPSHOT
   */

  /**
   * Constructs a new <code>FileStoreInfo</code>.
   * @alias module:org.geogig.web/org.geogig.web.model/FileStoreInfo
   * @class
   * @extends module:org.geogig.web/org.geogig.web.model/StoreConnectionInfo
   * @param type {String} 
   */
  var exports = function(type) {
    var _this = this;
    StoreConnectionInfo.call(_this, type);

  };

  /**
   * Constructs a <code>FileStoreInfo</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:org.geogig.web/org.geogig.web.model/FileStoreInfo} obj Optional instance to populate.
   * @return {module:org.geogig.web/org.geogig.web.model/FileStoreInfo} The populated <code>FileStoreInfo</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();
      StoreConnectionInfo.constructFromObject(data, obj);
      if (data.hasOwnProperty('directory')) {
        obj['directory'] = ApiClient.convertToType(data['directory'], 'String');
      }
    }
    return obj;
  }

  exports.prototype = Object.create(StoreConnectionInfo.prototype);
  exports.prototype.constructor = exports;

  /**
   * File system directory on the server where the repositories are stored
   * @member {String} directory
   */
  exports.prototype['directory'] = undefined;



  return exports;
}));


