class VoiceEntry {
  final String id;
  final String alias;
  final DateTime createdAt;
  final String? filePath;

  VoiceEntry({
    required this.id,
    required this.alias,
    required this.createdAt,
    this.filePath,
  });

  VoiceEntry copyWith({
    String? id,
    String? alias,
    DateTime? createdAt,
    String? filePath,
  }) {
    return VoiceEntry(
      id: id ?? this.id,
      alias: alias ?? this.alias,
      createdAt: createdAt ?? this.createdAt,
      filePath: filePath ?? this.filePath,
    );
  }
}
