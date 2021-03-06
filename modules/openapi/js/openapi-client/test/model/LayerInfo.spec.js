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
    instance = new Geogig.LayerInfo();
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

  describe('LayerInfo', function() {
    it('should create an instance of LayerInfo', function() {
      // uncomment below and update the code to test LayerInfo
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be.a(Geogig.LayerInfo);
    });

    it('should have the property head (base name: "head")', function() {
      // uncomment below and update the code to test the property head
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be();
    });

    it('should have the property name (base name: "name")', function() {
      // uncomment below and update the code to test the property name
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be();
    });

    it('should have the property title (base name: "title")', function() {
      // uncomment below and update the code to test the property title
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be();
    });

    it('should have the property _abstract (base name: "abstract")', function() {
      // uncomment below and update the code to test the property _abstract
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be();
    });

    it('should have the property size (base name: "size")', function() {
      // uncomment below and update the code to test the property size
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be();
    });

    it('should have the property bounds (base name: "bounds")', function() {
      // uncomment below and update the code to test the property bounds
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be();
    });

    it('should have the property type (base name: "type")', function() {
      // uncomment below and update the code to test the property type
      //var instane = new Geogig.LayerInfo();
      //expect(instance).to.be();
    });

  });

}));
