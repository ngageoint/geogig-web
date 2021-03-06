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
    define(['ApiClient', 'model/IdentifiedObject', 'model/StoreConnectionInfo'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('./IdentifiedObject'), require('./StoreConnectionInfo'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.StoreInfo = factory(root.Geogig.ApiClient, root.Geogig.IdentifiedObject, root.Geogig.StoreConnectionInfo);
  }
}(this, function(ApiClient, IdentifiedObject, StoreConnectionInfo) {
  'use strict';




  /**
   * The StoreInfo model module.
   * @module model/StoreInfo
   * @version 0.1-SNAPSHOT
   */

  /**
   * Constructs a new <code>StoreInfo</code>.
   * @alias module:model/StoreInfo
   * @class
   * @implements module:model/IdentifiedObject
   * @param enabled {Boolean} 
   */
  var exports = function(enabled) {
    var _this = this;

    IdentifiedObject.call(_this);

    _this['enabled'] = enabled;

  };

  /**
   * Constructs a <code>StoreInfo</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/StoreInfo} obj Optional instance to populate.
   * @return {module:model/StoreInfo} The populated <code>StoreInfo</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();

      IdentifiedObject.constructFromObject(data, obj);
      if (data.hasOwnProperty('description')) {
        obj['description'] = ApiClient.convertToType(data['description'], 'String');
      }
      if (data.hasOwnProperty('enabled')) {
        obj['enabled'] = ApiClient.convertToType(data['enabled'], 'Boolean');
      }
      if (data.hasOwnProperty('connectionInfo')) {
        obj['connectionInfo'] = StoreConnectionInfo.constructFromObject(data['connectionInfo']);
      }
    }
    return obj;
  }

  /**
   * @member {String} description
   */
  exports.prototype['description'] = undefined;
  /**
   * @member {Boolean} enabled
   * @default true
   */
  exports.prototype['enabled'] = true;
  /**
   * @member {module:model/StoreConnectionInfo} connectionInfo
   */
  exports.prototype['connectionInfo'] = undefined;

  // Implement IdentifiedObject interface:
  /**
   * A unique and immutable identifier for the object, generated by the system, and unique across the whole system
   * @member {String} id
   */
exports.prototype['id'] = undefined;

  /**
   * A user given name for the object, unique across its domain of usage, and modifiable as long as it remains unique. I.e. Store and User names are globally unique, Repository names are unique in the scope of their owner.
   * @member {String} identity
   */
exports.prototype['identity'] = undefined;



  return exports;
}));


