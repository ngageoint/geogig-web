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
    instance = new Geogig.UserInfoPublicProfile();
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

  describe('UserInfoPublicProfile', function() {
    it('should create an instance of UserInfoPublicProfile', function() {
      // uncomment below and update the code to test UserInfoPublicProfile
      //var instane = new Geogig.UserInfoPublicProfile();
      //expect(instance).to.be.a(Geogig.UserInfoPublicProfile);
    });

    it('should have the property avatarUrl (base name: "avatar_url")', function() {
      // uncomment below and update the code to test the property avatarUrl
      //var instane = new Geogig.UserInfoPublicProfile();
      //expect(instance).to.be();
    });

    it('should have the property gravatarId (base name: "gravatar_id")', function() {
      // uncomment below and update the code to test the property gravatarId
      //var instane = new Geogig.UserInfoPublicProfile();
      //expect(instance).to.be();
    });

    it('should have the property company (base name: "company")', function() {
      // uncomment below and update the code to test the property company
      //var instane = new Geogig.UserInfoPublicProfile();
      //expect(instance).to.be();
    });

    it('should have the property publicRepos (base name: "public_repos")', function() {
      // uncomment below and update the code to test the property publicRepos
      //var instane = new Geogig.UserInfoPublicProfile();
      //expect(instance).to.be();
    });

    it('should have the property followers (base name: "followers")', function() {
      // uncomment below and update the code to test the property followers
      //var instane = new Geogig.UserInfoPublicProfile();
      //expect(instance).to.be();
    });

    it('should have the property following (base name: "following")', function() {
      // uncomment below and update the code to test the property following
      //var instane = new Geogig.UserInfoPublicProfile();
      //expect(instance).to.be();
    });

  });

}));