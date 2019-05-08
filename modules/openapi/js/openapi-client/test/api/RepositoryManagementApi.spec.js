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
    instance = new Geogig.RepositoryManagementApi();
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

  describe('RepositoryManagementApi', function() {
    describe('countWatchers', function() {
      it('should call countWatchers successfully', function(done) {
        //uncomment below and update the code to test countWatchers
        //instance.countWatchers(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('createRepository', function() {
      it('should call createRepository successfully', function(done) {
        //uncomment below and update the code to test createRepository
        //instance.createRepository(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('deleteRepository', function() {
      it('should call deleteRepository successfully', function(done) {
        //uncomment below and update the code to test deleteRepository
        //instance.deleteRepository(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('forkRepository', function() {
      it('should call forkRepository successfully', function(done) {
        //uncomment below and update the code to test forkRepository
        //instance.forkRepository(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('getRepository', function() {
      it('should call getRepository successfully', function(done) {
        //uncomment below and update the code to test getRepository
        //instance.getRepository(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('listForks', function() {
      it('should call listForks successfully', function(done) {
        //uncomment below and update the code to test listForks
        //instance.listForks(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('listRepositories', function() {
      it('should call listRepositories successfully', function(done) {
        //uncomment below and update the code to test listRepositories
        //instance.listRepositories(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('listUserRepositories', function() {
      it('should call listUserRepositories successfully', function(done) {
        //uncomment below and update the code to test listUserRepositories
        //instance.listUserRepositories(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('listWatchers', function() {
      it('should call listWatchers successfully', function(done) {
        //uncomment below and update the code to test listWatchers
        //instance.listWatchers(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('modifyRepository', function() {
      it('should call modifyRepository successfully', function(done) {
        //uncomment below and update the code to test modifyRepository
        //instance.modifyRepository(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('unwatchRepository', function() {
      it('should call unwatchRepository successfully', function(done) {
        //uncomment below and update the code to test unwatchRepository
        //instance.unwatchRepository(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
    describe('watchRepository', function() {
      it('should call watchRepository successfully', function(done) {
        //uncomment below and update the code to test watchRepository
        //instance.watchRepository(function(error) {
        //  if (error) throw error;
        //expect().to.be();
        //});
        done();
      });
    });
  });

}));
