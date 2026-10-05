import React, {useEffect} from 'react';
import {View, StyleSheet, Animated, Easing} from 'react-native';

interface AnimatedVisualizerProps {
  size: number;
}

const AnimatedVisualizer: React.FC<AnimatedVisualizerProps> = ({size}) => {
  const rotation1 = new Animated.Value(0);
  const rotation2 = new Animated.Value(0);
  const rotation3 = new Animated.Value(0);
  const rotation4 = new Animated.Value(0);
  const scale = new Animated.Value(1);
  const opacity = new Animated.Value(0.9);

  useEffect(() => {
    // Create continuous rotation animations at different speeds
    const rotate1 = Animated.loop(
      Animated.timing(rotation1, {
        toValue: 1,
        duration: 6000,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );

    const rotate2 = Animated.loop(
      Animated.timing(rotation2, {
        toValue: 1,
        duration: 8000,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );

    const rotate3 = Animated.loop(
      Animated.timing(rotation3, {
        toValue: 1,
        duration: 10000,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );

    const rotate4 = Animated.loop(
      Animated.timing(rotation4, {
        toValue: 1,
        duration: 12000,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );

    // Create pulsing animation
    const pulse = Animated.loop(
      Animated.sequence([
        Animated.timing(scale, {
          toValue: 1.15,
          duration: 2500,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(scale, {
          toValue: 1,
          duration: 2500,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    );

    // Create opacity animation
    const fade = Animated.loop(
      Animated.sequence([
        Animated.timing(opacity, {
          toValue: 1,
          duration: 2000,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(opacity, {
          toValue: 0.7,
          duration: 2000,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    );

    rotate1.start();
    rotate2.start();
    rotate3.start();
    rotate4.start();
    pulse.start();
    fade.start();

    return () => {
      rotate1.stop();
      rotate2.stop();
      rotate3.stop();
      rotate4.stop();
      pulse.stop();
      fade.stop();
    };
  }, []);

  const rotate1Interpolate = rotation1.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

  const rotate2Interpolate = rotation2.interpolate({
    inputRange: [0, 1],
    outputRange: ['360deg', '0deg'],
  });

  const rotate3Interpolate = rotation3.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

  const rotate4Interpolate = rotation4.interpolate({
    inputRange: [0, 1],
    outputRange: ['180deg', '540deg'],
  });

  // Create arc segments for swirling effect
  const createArc = (radius: number, startAngle: number, endAngle: number, index: number) => {
    return (
      <View
        key={index}
        style={[
          styles.arc,
          {
            width: radius * 2,
            height: radius * 2,
            borderRadius: radius,
            borderWidth: 2,
            borderColor: '#00FF88',
            borderRightColor: 'transparent',
            borderBottomColor: 'transparent',
            transform: [{rotate: `${startAngle}deg`}],
          },
        ]}
      />
    );
  };

  return (
    <Animated.View
      style={[
        styles.container,
        {
          width: size,
          height: size,
          transform: [{scale}],
          opacity,
        },
      ]}>
      {/* Outer swirling arcs */}
      <Animated.View
        style={[
          styles.ring,
          {
            width: size,
            height: size,
            transform: [{rotate: rotate1Interpolate}],
          },
        ]}>
        {createArc(size / 2, 0, 180, 1)}
        {createArc(size / 2, 180, 360, 2)}
      </Animated.View>

      {/* Second layer */}
      <Animated.View
        style={[
          styles.ring,
          {
            width: size * 0.85,
            height: size * 0.85,
            transform: [{rotate: rotate2Interpolate}],
          },
        ]}>
        {createArc((size * 0.85) / 2, 45, 225, 3)}
        {createArc((size * 0.85) / 2, 225, 405, 4)}
      </Animated.View>

      {/* Third layer */}
      <Animated.View
        style={[
          styles.ring,
          {
            width: size * 0.7,
            height: size * 0.7,
            transform: [{rotate: rotate3Interpolate}],
          },
        ]}>
        {createArc((size * 0.7) / 2, 90, 270, 5)}
        {createArc((size * 0.7) / 2, 270, 450, 6)}
      </Animated.View>

      {/* Fourth layer */}
      <Animated.View
        style={[
          styles.ring,
          {
            width: size * 0.55,
            height: size * 0.55,
            transform: [{rotate: rotate4Interpolate}],
          },
        ]}>
        {createArc((size * 0.55) / 2, 135, 315, 7)}
        {createArc((size * 0.55) / 2, 315, 495, 8)}
      </Animated.View>

      {/* Central glow with multiple layers */}
      <View style={[styles.centerGlow, {width: size * 0.35, height: size * 0.35}]} />
      <View style={[styles.centerGlowInner, {width: size * 0.2, height: size * 0.2}]} />
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  ring: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  arc: {
    position: 'absolute',
    borderStyle: 'solid',
  },
  centerGlow: {
    backgroundColor: '#00FF88',
    borderRadius: 1000,
    opacity: 0.4,
    shadowColor: '#00FF88',
    shadowOffset: {width: 0, height: 0},
    shadowOpacity: 1,
    shadowRadius: 30,
    elevation: 15,
  },
  centerGlowInner: {
    backgroundColor: '#00FF88',
    borderRadius: 1000,
    opacity: 0.6,
    shadowColor: '#00FF88',
    shadowOffset: {width: 0, height: 0},
    shadowOpacity: 1,
    shadowRadius: 15,
    elevation: 10,
  },
});

export default AnimatedVisualizer;
