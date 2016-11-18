/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/** @module vertx-web-js/mvel_template_engine */
var utils = require('vertx-js/util/utils');
var Buffer = require('vertx-js/buffer');
var TemplateEngine = require('vertx-web-js/template_engine');
var RoutingContext = require('vertx-web-js/routing_context');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JMVELTemplateEngine = io.vertx.ext.web.templ.MVELTemplateEngine;

/**
 A template engine that uses the Handlebars library.

 @class
*/
var MVELTemplateEngine = function(j_val) {

  var j_mVELTemplateEngine = j_val;
  var that = this;
  TemplateEngine.call(this, j_val);

  /**

   @public
   @param arg0 {RoutingContext} 
   @param arg1 {string} 
   @param arg2 {function} 
   */
  this.render = function(arg0, arg1, arg2) {
    var __args = arguments;
    if (__args.length === 3 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'string' && typeof __args[2] === 'function') {
      j_mVELTemplateEngine["render(io.vertx.ext.web.RoutingContext,java.lang.String,io.vertx.core.Handler)"](arg0._jdel, arg1, function(ar) {
      if (ar.succeeded()) {
        arg2(utils.convReturnVertxGen(Buffer, ar.result()), null);
      } else {
        arg2(null, ar.cause());
      }
    });
    } else throw new TypeError('function invoked with invalid arguments');
  };

  /**
   Set the extension for the engine

   @public
   @param extension {string} the extension 
   @return {MVELTemplateEngine} a reference to this for fluency
   */
  this.setExtension = function(extension) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'string') {
      return utils.convReturnVertxGen(MVELTemplateEngine, j_mVELTemplateEngine["setExtension(java.lang.String)"](extension));
    } else throw new TypeError('function invoked with invalid arguments');
  };

  /**
   Set the max cache size for the engine

   @public
   @param maxCacheSize {number} the maxCacheSize 
   @return {MVELTemplateEngine} a reference to this for fluency
   */
  this.setMaxCacheSize = function(maxCacheSize) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] ==='number') {
      return utils.convReturnVertxGen(MVELTemplateEngine, j_mVELTemplateEngine["setMaxCacheSize(int)"](maxCacheSize));
    } else throw new TypeError('function invoked with invalid arguments');
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_mVELTemplateEngine;
};

MVELTemplateEngine._jclass = utils.getJavaClass("io.vertx.ext.web.templ.MVELTemplateEngine");
MVELTemplateEngine._jtype = {
  accept: function(obj) {
    return MVELTemplateEngine._jclass.isInstance(obj._jdel);
  },
  wrap: function(jdel) {
    var obj = Object.create(MVELTemplateEngine.prototype, {});
    MVELTemplateEngine.apply(obj, arguments);
    return obj;
  },
  unwrap: function(obj) {
    return obj._jdel;
  }
};
MVELTemplateEngine._create = function(jdel) {
  var obj = Object.create(MVELTemplateEngine.prototype, {});
  MVELTemplateEngine.apply(obj, arguments);
  return obj;
}
/**
 Create a template engine using defaults

 @memberof module:vertx-web-js/mvel_template_engine

 @return {MVELTemplateEngine} the engine
 */
MVELTemplateEngine.create = function() {
  var __args = arguments;
  if (__args.length === 0) {
    return utils.convReturnVertxGen(MVELTemplateEngine, JMVELTemplateEngine["create()"]());
  } else throw new TypeError('function invoked with invalid arguments');
};

module.exports = MVELTemplateEngine;