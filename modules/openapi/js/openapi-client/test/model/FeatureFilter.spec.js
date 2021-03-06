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
    instance = new Geogig.FeatureFilter();
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

  describe('FeatureFilter', function() {
    it('should create an instance of FeatureFilter', function() {
      // uncomment below and update the code to test FeatureFilter
      //var instane = new Geogig.FeatureFilter();
      //expect(instance).to.be.a(Geogig.FeatureFilter);
    });

    it('should have the property featureIds (base name: "featureIds")', function() {
      // uncomment below and update the code to test the property featureIds
      //var instane = new Geogig.FeatureFilter();
      //expect(instance).to.be();
    });

    it('should have the property bbox (base name: "bbox")', function() {
      // uncomment below and update the code to test the property bbox
      //var instane = new Geogig.FeatureFilter();
      //expect(instance).to.be();
    });

    it('should have the property cqlFilter (base name: "cqlFilter")', function() {
      // uncomment below and update the code to test the property cqlFilter
      //var instane = new Geogig.FeatureFilter();
      //expect(instance).to.be();
    });

  });

}));
