#!/usr/bin/env python
import numpy as np
import pyglet
from pyglet.gl import *
import time

code = "00000000001010101010" #bit sequence to transmit
alpha = 0.9 #0.5
frame_rate = 2
frame_buffer_len = 6
code_len = len(code)
code_idx = 0 #where are we in current code
curr_bit = code[code_idx]
frame_idx = 0 #how many frames have we transmitted
curr_opacity = 100

def main():
	#original image
	image_file = 'seattle.png'
	img = pyglet.image.load(image_file)
	img_sprite = pyglet.sprite.Sprite(img)
	window = pyglet.window.Window()
	window.set_fullscreen()
	img_sprite.scale = 10.0

	label = pyglet.text.Label(curr_bit,
                          font_name='Times New Roman',
                          font_size=20,
                          x=window.width//2, y=window.height//2,
                          anchor_x='center', anchor_y='center')

	@window.event
	def on_draw():
		global curr_opacity, curr_bit
		window.clear()
		glEnable(GL_BLEND)
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		img_sprite.opacity = curr_opacity #alpha * 100
		img_sprite.draw()
		label.text = curr_bit
		label.draw()


	def update(dt):
		global curr_bit, code_idx, code, code_len, frame_idx, frame_buffer_len, curr_opacity
		
		print('Transmitting bit: {}'.format(curr_bit))
		if curr_bit == '0':
			if frame_idx in [0, 1, 3, 4]:
				curr_opacity = alpha*100
			else:
				curr_opacity = 100
		elif curr_bit == '1':
			if frame_idx in [0, 2, 4]:
				curr_opacity = alpha*100
			else:
				curr_opacity = 100
		frame_idx = (frame_idx + 1) % frame_buffer_len
		
		if frame_idx == 0:
			print('Bit transmitted over 6 frames')
			code_idx = (code_idx + 1) #% code_len
			
			if code_idx == code_len:
				print('Code transmitted exiting')
				time.sleep(1.0 / frame_rate)
				exit()
				# curr_opacity = 100 
			else:
				curr_bit = code[code_idx]

	pyglet.clock.schedule_interval(update, 1.0 / frame_rate)
	pyglet.app.run()

if __name__ == '__main__':
	input('Press enter to start...')
	main()