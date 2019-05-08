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
    define(['org.geogig.web/ApiClient', 'org.geogig.web/org.geogig.web.model/BoundingBox'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('./BoundingBox'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.TreeBucket = factory(root.Geogig.ApiClient, root.Geogig.BoundingBox);
  }
}(this, function(ApiClient, BoundingBox) {
  'use strict';




  /**
   * The TreeBucket model module.
   * @module org.geogig.web/org.geogig.web.model/TreeBucket
   * @version 1.0-SNAPSHOT
   */

  /**
   * Constructs a new <code>TreeBucket</code>.
   * @alias module:org.geogig.web/org.geogig.web.model/TreeBucket
   * @class
   */
  var exports = function() {
    var _this = this;




  };

  /**
   * Constructs a <code>TreeBucket</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:org.geogig.web/org.geogig.web.model/TreeBucket} obj Optional instance to populate.
   * @return {module:org.geogig.web/org.geogig.web.model/TreeBucket} The populated <code>TreeBucket</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();

      if (data.hasOwnProperty('index')) {
        obj['index'] = ApiClient.convertToType(data['index'], 'Number');
      }
      if (data.hasOwnProperty('objectId')) {
        obj['objectId'] = ApiClient.convertToType(data['objectId'], 'String');
      }
      if (data.hasOwnProperty('bounds')) {
        obj['bounds'] = BoundingBox.constructFromObject(data['bounds']);
      }
    }
    return obj;
  }

  /**
   * @member {Number} index
   */
  exports.prototype['index'] = undefined;
  /**
   * @member {String} objectId
   */
  exports.prototype['objectId'] = undefined;
  /**
   * @member {module:org.geogig.web/org.geogig.web.model/BoundingBox} bounds
   */
  exports.prototype['bounds'] = undefined;



  return exports;
}));


