(function(angular){
	'use strict';

	angular.module('Test',['ngCookies'])
	.controller('MainController', ['$scope', '$cookieStore',function($scope, $cookieStore) {
      
      $scope.blabla = "bkbk";      
      window.console.log($cookieStore);
      var obj = $cookieStore;
      var user = angular.fromJson(obj.get('user'));
      

      	
      window.console.log(obj["user"]);
      window.console.log(user);
      window.console.log(user.userName);
      

  }])
})(window.angular);