module.exports = {
	200: function () {
		console.log("Status code 200: Success.");
  },
  202: function () {
    console.log("Status code 202: Success for creation.");
  },
	400: function () {
		console.log("Error code 400: Bad Request.");
	},
	401: function () {
		console.log("Error code 401: Unauthorized.");
	},
	402: function () {
		console.log("Error code 402: Payment Required.");
	},
	403: function () {
		console.log("Error code 403: Forbidden.");
    App.router.logOff();
	},
	404: function () {
		console.log("Error code 404: URI not found.");
	},
	500: function () {
		console.log("Error code 500: Internal Error on server side.");
	},
	501: function () {
		console.log("Error code 501: Not implemented yet.");
	},
	502: function () {
		console.log("Error code 502: Services temporarily overloaded.");
	},
	503: function () {
		console.log("Error code 503: Gateway timeout.");
	}
}
