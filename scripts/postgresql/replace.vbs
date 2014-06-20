Dim FileName, Find, ReplaceWith, FileContents, dFileContents
Find         = WScript.Arguments(0)
ReplaceWith  = WScript.Arguments(1)
FileName     = WScript.Arguments(2)

FileContents = GetFile(FileName)

dFileContents = replace(FileContents, Find, ReplaceWith, 1, -1, 1)

if dFileContents <> FileContents Then
  WriteFile FileName, dFileContents
End If

function GetFile(FileName)
  If FileName<>"" Then
    Dim FS, FileStream
    Set FS = CreateObject("Scripting.FileSystemObject")
      on error resume Next
      Set FileStream = FS.OpenTextFile(FileName)
      GetFile = FileStream.ReadAll
  End If
End Function

function WriteFile(FileName, Contents)
  Dim OutStream, FS

  on error resume Next
  Set FS = CreateObject("Scripting.FileSystemObject")
    Set OutStream = FS.OpenTextFile(FileName, 2, True)
    OutStream.Write Contents
End Function

