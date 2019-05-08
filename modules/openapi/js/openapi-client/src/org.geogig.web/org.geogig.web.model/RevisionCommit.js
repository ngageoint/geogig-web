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
    root.Geogig.RevisionCommit = factory(root.Geogig.ApiClient, root.Geogig.ObjectType, root.Geogig.Person, root.Geogig.RevisionObject);
  }
}(this, function(ApiClient, ObjectType, Person, RevisionObject) {
  'use strict';




  /**
   * The RevisionCommit model module.
   * @module org.geogig.web/org.geogig.web.model/RevisionCommit
   * @version 1.0-SNAPSHOT
   */

  /**
   * Constructs a new <code>RevisionCommit</code>.
   * @alias module:org.geogig.web/org.geogig.web.model/RevisionCommit
   * @class
   * @extends module:org.geogig.web/org.geogig.web.model/RevisionObject
   * @param id {String} 
   * @param objectType {module:org.geogig.web/org.geogig.web.model/ObjectType} 
   * @param parentIds {Array.<String>} 
   * @param treeId {String} 
   * @param message {String} 
   * @param author {module:org.geogig.web/org.geogig.web.model/Person} 
   * @param committer {module:org.geogig.web/org.geogig.web.model/Person} 
   */
  var exports = function(id, objectType, parentIds, treeId, message, author, committer) {
    var _this = this;
    RevisionObject.call(_this, id, objectType);
    _this['parentIds'] = parentIds;
    _this['treeId'] = treeId;
    _this['message'] = message;
    _this['author'] = author;
    _this['committer'] = committer;
  };

  /**
   * Constructs a <code>RevisionCommit</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:org.geogig.web/org.geogig.web.model/RevisionCommit} obj Optional instance to populate.
   * @return {module:org.geogig.web/org.geogig.web.model/RevisionCommit} The populated <code>RevisionCommit</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();
      RevisionObject.constructFromObject(data, obj);
      if (data.hasOwnProperty('parentIds')) {
        obj['parentIds'] = ApiClient.convertToType(data['parentIds'], ['String']);
      }
      if (data.hasOwnProperty('treeId')) {
        obj['treeId'] = ApiClient.convertToType(data['treeId'], 'String');
      }
      if (data.hasOwnProperty('message')) {
        obj['message'] = ApiClient.convertToType(data['message'], 'String');
      }
      if (data.hasOwnProperty('author')) {
        obj['author'] = Person.constructFromObject(data['author']);
      }
      if (data.hasOwnProperty('committer')) {
        obj['committer'] = Person.constructFromObject(data['committer']);
      }
    }
    return obj;
  }

  exports.prototype = Object.create(RevisionObject.prototype);
  exports.prototype.constructor = exports;

  /**
   * @member {Array.<String>} parentIds
   */
  exports.prototype['parentIds'] = undefined;
  /**
   * @member {String} treeId
   */
  exports.prototype['treeId'] = undefined;
  /**
   * @member {String} message
   */
  exports.prototype['message'] = undefined;
  /**
   * @member {module:org.geogig.web/org.geogig.web.model/Person} author
   */
  exports.prototype['author'] = undefined;
  /**
   * @member {module:org.geogig.web/org.geogig.web.model/Person} committer
   */
  exports.prototype['committer'] = undefined;



  return exports;
}));


