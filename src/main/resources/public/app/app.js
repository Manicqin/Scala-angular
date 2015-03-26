(function(angular){
	'use strict';

	angular.module('main',['ngRoute','ngCookies','ui.bootstrap'])
	.controller('UsersController', ['$scope', '$http', '$route',function($scope,$http,$route, $routeParams, $location) {
  		$scope.$route = $route;
    	$scope.$location = $location;
 		$scope.$routeParams = $routeParams;

  		$scope.data = [];

  		$http.get('http://127.0.0.1:7070/printDB?db=users')
  			.success(function(data){
  				$scope.data = data;
  			});

	}])
  .controller('MainController', ['$scope', '$cookieStore',function($scope, $cookieStore) {
      
      $scope.username = "";      
      var obj = angular.fromJson($cookieStore.get('user'));
      $scope.username = obj.userName;

  }])
  .controller('SigninCtrl', ['$scope', '$http', '$window','$route',function ($scope,$http,$window) {
    $scope.passwordText = 'Password';
    $scope.usernameText = 'User Name';
    $scope.alerts = [];
    
    $scope.login = function() {
      
      $http.post('http://127.0.0.1:7070/login',{user:$scope.usernameText,password:$scope.passwordText})
        .success(function(data, status, headers, config){          
          var obj = angular.fromJson(data);
          // alert(obj["userName"]);
          //alert(obj["logged"]);

          if(obj["logged"] == true){
            $scope.alerts.push({msg:"Hello " + obj["userName"] ,type: 'success'});
            window.setTimeout(function() {
              $window.location.href = "http://127.0.0.1:7070/";
           }, 1000);
          }
          else{
            $scope.alerts.push({msg:"Error please check password or login" ,type: 'danger'});
          }
          
        })
        .error(function(data, status, headers, config) {
          // called asynchronously if an error occurs
          // or server returns response with an error status.
        });
    };

    $scope.closeAlert = function(index) {
      debugger;
      $scope.alerts.splice(index, 1);
      

    };
  }])
	.controller('AssetsController', ['$scope', '$http', '$route',function($scope,$http,$route, $routeParams, $location) {
  		$scope.$route = $route;
    	$scope.$location = $location;
 		$scope.$routeParams = $routeParams;

  		$scope.data = [];

  		$http.get('http://127.0.0.1:7070/printDB?db=assets')
  			.success(function(data){  				
  				$scope.data = data;
  			});

	}])
	.controller('OwnersController', ['$scope', '$http', '$route',function($scope,$http,$route, $routeParams, $location) {
  		$scope.$route = $route;
    	$scope.$location = $location;
 		$scope.$routeParams = $routeParams;

  		$scope.data = [];

  		$http.get('http://127.0.0.1:7070/printDB?db=owners')
  			.success(function(data){
  				$scope.data = data;
  			});

	}])
	.config(function($routeProvider, $locationProvider) {
  		$routeProvider
   		.when('/goto/Users', {
    		templateUrl: 'users.html',
    		controller: 'UsersController'
  		})
  		.when('/goto/Assets', {
    		templateUrl: 'assets.html',
    		controller: 'AssetsController'
  		})
  		.when('/goto/Owners', {
    		templateUrl: 'owners.html',
    		controller: 'OwnersController'
  		});
  		// configure html5 to get links working on jsfiddle
  		$locationProvider.html5Mode(true);
  	});
})(window.angular);