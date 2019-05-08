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
    // AMD.
    define(['expect.js', '../../src/index'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    factory(require('expect.js'), require('../../src/index'));
  } else {
    // Browser globals (root is window)
    factory(root.expect, root.Geogig);
  }
}(this, function(expect, Geogig) {
  'use strict';

  var instance;

  beforeEach(function() {
    instance = new Geogig.RevisionCommit();
  });

  var getProperty = function(object, getter, property) {
    // Use getter method if present; otherwise, get the property directly.
    if (typeof object[getter] === 'function')
      return object[getter]();
    else
      return object[property];
  }

  var setProperty = function(object, setter, property, value) {
    // Use setter method if present; otherwise, set the property directly.
    if (typeof object[setter] === 'function')
      object[setter](value);
    else
      object[property] = value;
  }

  describe('RevisionCommit', function() {
    it('should create an instance of RevisionCommit', function() {
      // uncomment below and update the code to test RevisionCommit
      //var instane = new Geogig.RevisionCommit();
      //expect(instance).to.be.a(Geogig.RevisionCommit);
    });

    it('should have the property parentIds (base name: "parentIds")', function() {
      // uncomment below and update the code to test the property parentIds
      //var instane = new Geogig.RevisionCommit();
      //expect(instance).to.be();
    });

    it('should have the property treeId (base name: "treeId")', function() {
      // uncomment below and update the code to test the property treeId
      //var instane = new Geogig.RevisionCommit();
      //expect(instance).to.be();
    });

    it('should have the property message (base name: "message")', function() {
      // uncomment below and update the code to test the property message
      //var instane = new Geogig.RevisionCommit();
      //expect(instance).to.be();
    });

    it('should have the property author (base name: "author")', function() {
      // uncomment below and update the code to test the property author
      //var instane = new Geogig.RevisionCommit();
      //expect(instance).to.be();
    });

    it('should have the property committer (base name: "committer")', function() {
      // uncomment below and update the code to test the property committer
      //var instane = new Geogig.RevisionCommit();
      //expect(instance).to.be();
    });

  });

}));
