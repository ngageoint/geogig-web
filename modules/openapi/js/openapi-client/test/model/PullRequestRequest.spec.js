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
    instance = new Geogig.PullRequestRequest();
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

  describe('PullRequestRequest', function() {
    it('should create an instance of PullRequestRequest', function() {
      // uncomment below and update the code to test PullRequestRequest
      //var instane = new Geogig.PullRequestRequest();
      //expect(instance).to.be.a(Geogig.PullRequestRequest);
    });

    it('should have the property title (base name: "title")', function() {
      // uncomment below and update the code to test the property title
      //var instane = new Geogig.PullRequestRequest();
      //expect(instance).to.be();
    });

    it('should have the property description (base name: "description")', function() {
      // uncomment below and update the code to test the property description
      //var instane = new Geogig.PullRequestRequest();
      //expect(instance).to.be();
    });

    it('should have the property targetBranch (base name: "targetBranch")', function() {
      // uncomment below and update the code to test the property targetBranch
      //var instane = new Geogig.PullRequestRequest();
      //expect(instance).to.be();
    });

    it('should have the property sourceRepositoryOwner (base name: "sourceRepositoryOwner")', function() {
      // uncomment below and update the code to test the property sourceRepositoryOwner
      //var instane = new Geogig.PullRequestRequest();
      //expect(instance).to.be();
    });

    it('should have the property sourceRepositryName (base name: "sourceRepositryName")', function() {
      // uncomment below and update the code to test the property sourceRepositryName
      //var instane = new Geogig.PullRequestRequest();
      //expect(instance).to.be();
    });

    it('should have the property sourceRepositoryBranch (base name: "sourceRepositoryBranch")', function() {
      // uncomment below and update the code to test the property sourceRepositoryBranch
      //var instane = new Geogig.PullRequestRequest();
      //expect(instance).to.be();
    });

  });

}));
