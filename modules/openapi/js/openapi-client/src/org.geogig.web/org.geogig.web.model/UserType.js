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
    define(['org.geogig.web/ApiClient'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.UserType = factory(root.Geogig.ApiClient);
  }
}(this, function(ApiClient) {
  'use strict';


  /**
   * Enum class UserType.
   * @enum {}
   * @readonly
   */
  var exports = {
    /**
     * value: "INDIVIDUAL"
     * @const
     */
    "INDIVIDUAL": "INDIVIDUAL",
    /**
     * value: "ORGANIZATION"
     * @const
     */
    "ORGANIZATION": "ORGANIZATION"  };

  /**
   * Returns a <code>UserType</code> enum value from a Javascript object name.
   * @param {Object} data The plain JavaScript object containing the name of the enum value.
   * @return {module:org.geogig.web/org.geogig.web.model/UserType} The enum <code>UserType</code> value.
   */
  exports.constructFromObject = function(object) {
    return object;
  }

  return exports;
}));


