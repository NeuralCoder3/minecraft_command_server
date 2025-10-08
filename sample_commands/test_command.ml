(*
  Alternatively, one could use the yojson ppx to derive the JSON serialization.
  eval $(opam env)
  ocamlfind ocamlopt -package unix,yojson -linkpkg -o test_command.out test_command.ml && ./test_command.out
*)

open Unix

(* Type definitions are now plain OCaml, with no annotations. *)
type block_data = { x : int; y : int; z : int; block : string }

type command =
  | Message of { command : string; text : string }
  | PlaceBlocks of { command : string; blocks : block_data list }

(** [command_to_yojson cmd] manually converts a command variant to a JSON object. *)
let command_to_yojson (cmd : command) : Yojson.Safe.t =
  let block_data_to_yojson b = (* Helper is now local to this function *)
    `Assoc [("x", `Int b.x); ("y", `Int b.y); ("z", `Int b.z); ("block", `String b.block)]
  in
  match cmd with
  | Message msg -> `Assoc [("command", `String msg.command); ("text", `String msg.text)]
  | PlaceBlocks pb ->
    `Assoc [
      ("command", `String pb.command);
      ("blocks", `List (List.map block_data_to_yojson pb.blocks));
    ]

(** [send_command host port command] connects, serializes, and sends a command. *)
let send_command ?(host="localhost") ?(port=8080) (cmd : command) =
  let sock = socket PF_INET SOCK_STREAM 0 in
  try
    let addr = (gethostbyname host).h_addr_list.(0) in
    connect sock (ADDR_INET (addr, port));
    let out_chan = out_channel_of_descr sock in
    cmd |> command_to_yojson |> Yojson.Safe.to_string |> Printf.fprintf out_chan "%s\n";
    flush out_chan;
    let response = input_line (in_channel_of_descr sock) in
    Printf.printf "Response: %s\n" response;
    close sock
  with
  | Unix_error (ECONNREFUSED, _, _) ->
      Printf.eprintf "Connection to %s:%d refused. Is the server running?\n" host port;
      close sock
  | Not_found ->
      Printf.eprintf "Error: Could not resolve hostname '%s'.\n" host;
      close sock
  | ex ->
      Printf.eprintf "An error occurred: %s\n" (Printexc.to_string ex);
      close sock

(* Main execution *)
let () =
  print_endline "Server Mod Command Test Script (OCaml)";
  print_endline "====================================";

  print_endline "\n1. Testing with message:";
  Message { command = "message"; text = "This is a test message from OCaml! (No PPX)" }
  |> send_command;

  print_endline "\n2. Testing placeblocks command:";
  PlaceBlocks {
    command = "placeblocks";
    blocks = [
      { x = 0; y = 130; z = 10; block = "diamond_block" };
      { x = 1; y = 130; z = 10; block = "gold_block" };
      { x = 2; y = 130; z = 10; block = "emerald_block" };
      { x = 3; y = 130; z = 10; block = "lapis_block" };
    ]
  }
  |> send_command;

  print_endline "\nDone! Check your Minecraft server console for the messages and world for placed blocks."

