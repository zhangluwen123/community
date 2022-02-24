$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");
	$("#hintModal").modal("show");

	var toName = $("#recipient-name").val();
	var content = $("#message-text").val();

	$.post(
		MY_PATH + "/letter/add/",
		{"toName":toName, "content":content},
		function (data){

			data = $.parseJSON(data);
			$("#hintBody").text(data.msg);
			setTimeout(function(){
				$("#hintModal").modal("hide");
				location.reload();
			}, 2000);
		}
	)
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}
