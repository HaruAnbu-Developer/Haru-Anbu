import 'package:flutter/material.dart';
import '../theme/app_text_styles.dart';

class GpsMapCard extends StatelessWidget {
  final String lastUpdated;
  final VoidCallback? onTap;

  const GpsMapCard({super.key, required this.lastUpdated, this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: SizedBox(
          height: 286,
          width: double.infinity,
          child: Stack(
            children: [
              // Map image
              Positioned.fill(
                child: Image.asset(
                  'assets/images/map_image.png',
                  fit: BoxFit.cover,
                ),
              ),
              // Text content
              Positioned(
                top: 17,
                left: 16,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('실시간 GPS 위치', style: AppTextStyles.mapTitle),
                    Text(lastUpdated, style: AppTextStyles.mapSubtitle),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
