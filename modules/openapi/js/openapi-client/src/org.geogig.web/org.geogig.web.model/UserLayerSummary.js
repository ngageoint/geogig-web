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
    define(['org.geogig.web/ApiClient', 'org.geogig.web/org.geogig.web.model/RepoLayerSummary'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('./RepoLayerSummary'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.UserLayerSummary = factory(root.Geogig.ApiClient, root.Geogig.RepoLayerSummary);
  }
}(this, function(ApiClient, RepoLayerSummary) {
  'use strict';




  /**
   * The UserLayerSummary model module.
   * @module org.geogig.web/org.geogig.web.model/UserLayerSummary
   * @version 1.0-SNAPSHOT
   */

  /**
   * Constructs a new <code>UserLayerSummary</code>.
   * Single Layer metadata at a given head/branch
   * @alias module:org.geogig.web/org.geogig.web.model/UserLayerSummary
   * @class
   */
  var exports = function() {
    var _this = this;



  };

  /**
   * Constructs a <code>UserLayerSummary</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:org.geogig.web/org.geogig.web.model/UserLayerSummary} obj Optional instance to populate.
   * @return {module:org.geogig.web/org.geogig.web.model/UserLayerSummary} The populated <code>UserLayerSummary</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();

      if (data.hasOwnProperty('userName')) {
        obj['userName'] = ApiClient.convertToType(data['userName'], 'String');
      }
      if (data.hasOwnProperty('repositories')) {
        obj['repositories'] = ApiClient.convertToType(data['repositories'], [RepoLayerSummary]);
      }
    }
    return obj;
  }

  /**
   * @member {String} userName
   */
  exports.prototype['userName'] = undefined;
  /**
   * @member {Array.<module:org.geogig.web/org.geogig.web.model/RepoLayerSummary>} repositories
   */
  exports.prototype['repositories'] = undefined;



  return exports;
}));


