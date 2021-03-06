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
    define(['org.geogig.web/ApiClient', 'org.geogig.web/org.geogig.web.model/ObjectType', 'org.geogig.web/org.geogig.web.model/Person', 'org.geogig.web/org.geogig.web.model/RevisionObject'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'), require('./ObjectType'), require('./Person'), require('./RevisionObject'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.RevisionTag = factory(root.Geogig.ApiClient, root.Geogig.ObjectType, root.Geogig.Person, root.Geogig.RevisionObject);
  }
}(this, function(ApiClient, ObjectType, Person, RevisionObject) {
  'use strict';




  /**
   * The RevisionTag model module.
   * @module org.geogig.web/org.geogig.web.model/RevisionTag
   * @version 1.0-SNAPSHOT
   */

  /**
   * Constructs a new <code>RevisionTag</code>.
   * @alias module:org.geogig.web/org.geogig.web.model/RevisionTag
   * @class
   * @extends module:org.geogig.web/org.geogig.web.model/RevisionObject
   * @param id {String} 
   * @param objectType {module:org.geogig.web/org.geogig.web.model/ObjectType} 
   * @param name {String} 
   * @param message {String} 
   * @param commitId {String} 
   * @param tagger {module:org.geogig.web/org.geogig.web.model/Person} 
   */
  var exports = function(id, objectType, name, message, commitId, tagger) {
    var _this = this;
    RevisionObject.call(_this, id, objectType);
    _this['name'] = name;
    _this['message'] = message;
    _this['commitId'] = commitId;
    _this['tagger'] = tagger;
  };

  /**
   * Constructs a <code>RevisionTag</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:org.geogig.web/org.geogig.web.model/RevisionTag} obj Optional instance to populate.
   * @return {module:org.geogig.web/org.geogig.web.model/RevisionTag} The populated <code>RevisionTag</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();
      RevisionObject.constructFromObject(data, obj);
      if (data.hasOwnProperty('name')) {
        obj['name'] = ApiClient.convertToType(data['name'], 'String');
      }
      if (data.hasOwnProperty('message')) {
        obj['message'] = ApiClient.convertToType(data['message'], 'String');
      }
      if (data.hasOwnProperty('commitId')) {
        obj['commitId'] = ApiClient.convertToType(data['commitId'], 'String');
      }
      if (data.hasOwnProperty('tagger')) {
        obj['tagger'] = Person.constructFromObject(data['tagger']);
      }
    }
    return obj;
  }

  exports.prototype = Object.create(RevisionObject.prototype);
  exports.prototype.constructor = exports;

  /**
   * @member {String} name
   */
  exports.prototype['name'] = undefined;
  /**
   * @member {String} message
   */
  exports.prototype['message'] = undefined;
  /**
   * @member {String} commitId
   */
  exports.prototype['commitId'] = undefined;
  /**
   * @member {module:org.geogig.web/org.geogig.web.model/Person} tagger
   */
  exports.prototype['tagger'] = undefined;



  return exports;
}));


