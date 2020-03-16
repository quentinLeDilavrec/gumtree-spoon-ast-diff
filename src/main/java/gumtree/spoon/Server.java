package gumtree.spoon;

import static spark.Spark.exception;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.put;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.github.gumtreediff.actions.model.Insert;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gumtree.spoon.builder.Json4SpoonGenerator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;

final class Server {
	static void server(String[] args) throws Exception {
		Set<String> validOrigins = new HashSet<String>();
		validOrigins.add("http://127.0.0.1:8080");
		validOrigins.add("http://localhost:8080");
		validOrigins.add("http://127.0.0.1:8081");
		validOrigins.add("http://localhost:8081");
		validOrigins.add("http://131.254.17.96:8080");

		int serverport = 8087;
		if (args.length <= 1 || Objects.equals(args[0], "server")) {
			if ((args.length == 1 && !Objects.equals(args[0], "server")) || args.length > 1) {
				serverport = Integer.valueOf(args[1]);
			}
			System.out.println("Launching server on  " + serverport);
			port(serverport);

			options("/spoon", (req, res) -> {
				System.out.println("====options=====spoon=========");
				System.out.println(req.headers("Origin"));
				res.header("Content-Type", "application/json");
				if (validOrigins.contains(req.headers("Origin"))) {
					res.header("Access-Control-Allow-Origin", req.headers("Origin"));
				}
				res.header("Access-Control-Allow-Credentials", "true");
				res.header("Access-Control-Allow-Methods", "OPTIONS,PUT");
				res.header("Access-Control-Allow-Headers",
						"Origin, Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
				return "";
			});
			options("/gumtree", (req, res) -> {
				System.out.println("====options=====gumtree=========");
				System.out.println(req.headers("Origin"));
				res.header("Content-Type", "application/json");
				if (validOrigins.contains(req.headers("Origin"))) {
					res.header("Access-Control-Allow-Origin", req.headers("Origin"));
				}
				// res.header("Access-Control-Allow-Origin", "*");
				res.header("Access-Control-Allow-Credentials", "true");
				res.header("Access-Control-Allow-Methods", "OPTIONS,PUT");
				res.header("Access-Control-Allow-Headers",
						"Origin,Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
				return "";
			});

			put("/spoon", (req, res) -> {
				System.out.println("=========spoon=========");
				System.out.println(req.headers("Origin"));
				res.header("Content-Type", "application/json");
				if (validOrigins.contains(req.headers("Origin"))) {
					res.header("Access-Control-Allow-Origin", req.headers("Origin"));
				}
				// res.header("Access-Control-Allow-Origin", "*");
				res.header("Access-Control-Allow-Credentials", "true");
				res.header("Access-Control-Allow-Methods", "OPTIONS,PUT");

				// // Make sure it's a valid origin
				// if (validOrigins. (req.origin) != -1) {
				// // Set the header to the requested origin
				// res.header('Access-Control-Allow-Origin', origin);
				// }
				// // res.header("Access-Control-Allow-Origin", "http://127.0.0.1:8080"); //
				// TODO accept more than one origin
				try {
					// String code = new
					// String(Base64.getDecoder().decode(req.queryParams("code")));
					String code = new String(Base64.getDecoder().decode(req.body()));
					System.err.println(code);

					Json4SpoonGenerator x = new Json4SpoonGenerator();
					AstComparator comp = new AstComparator();
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					JsonObject r = x.getJSONasJsonObject(comp.getCtType(code));

					System.err.println(comp.getCtType(code));
					System.err.println(r);
					// return "{}";
					return gson.toJson(r);
				} catch (Exception ee) {
					System.err.println(ee);
					return "{\"error\":\"" + ee.toString() + "\"}";
				}
			});
			put("/gumtree", (req, res) -> {
				System.out.println("=========gumtree=========");
				res.header("Content-Type", "application/json");
				System.out.println(req.headers("Origin"));
				if (validOrigins.contains(req.headers("Origin"))) {
					res.header("Access-Control-Allow-Origin", req.headers("Origin"));
				}
				res.header("Access-Control-Allow-Credentials", "true");
				res.header("Access-Control-Allow-Methods", "OPTIONS,PUT");
				try {
					String[] body = req.body().split("\n", 2);
					// String old = new String(Base64.getDecoder().decode(req.queryParams("old")));
					// String neww = new String(Base64.getDecoder().decode(req.queryParams("new")));
					String old = new String(Base64.getDecoder().decode(body[0]));
					String neww = new String(Base64.getDecoder().decode(body[1]));
					System.err.println(old);
					System.err.println(neww);

					AstComparator comp = new AstComparator();
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					final Diff diff = comp.compare(old, neww);
					// System.out.println(old);
					// System.out.println(neww);
					// System.out.println(diff.getAllOperations().get(0));
					JsonElement o = comp.formatDiff(diff.getRootOperations());

					System.out.println(o);
					return gson.toJson(o);
				} catch (Exception ee) {
					System.err.println(ee);
					return "{\"error\":\"" + ee.toString() + "\"}";
				}
			});
			// internalServerError((req, res) -> {
			// System.out.println("Error");
			// System.out.println(req);
			// System.out.println(res);
			// res.type("application/json");
			// return "{\"message\":\"Custom 500 handling\"}";
			// });
			exception(Exception.class, (exception, request, response) -> {
				// Handle the exception here
				System.out.println("Exception");
				System.out.println(exception.getMessage());
				System.out.println(request);
				System.out.println(response);
				response.status(400);
			});
		} else if (Objects.equals(args[0], "cli")) {
			File fl = new File(args[1]);
			if (args.length == 2) {

				Json4SpoonGenerator x = new Json4SpoonGenerator();
				AstComparator comp = new AstComparator();
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonObject r = x.getJSONasJsonObject(comp.getCtType(fl));
				System.out.println(gson.toJson(r));
			} else {
				File fr = new File(args[2]);

				AstComparator comp = new AstComparator();
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				final Diff diff = comp.compare(fl, fr);
				// System.out.println(diff);
				// System.out.println(neww);
				// System.out.println(diff.getAllOperations().get(0));
				List<Operation> ops = new ArrayList<Operation>();
				for (Operation tch : diff.getRootOperations()) {
					if (tch.getAction() instanceof Insert) {
						ops.add(tch);
					}
				}
				JsonElement o = comp.formatDiff(ops);
				// System.out.println(gson.toJson(o.getAsJsonObject().get("actions").getAsJsonArray().get(0)));
				System.out.println(gson.toJson(o));
			}
		}
	}
}